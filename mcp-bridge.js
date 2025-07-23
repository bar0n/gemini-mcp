#!/usr/bin/env node

const http = require('http');

// MCP server implementation
class GeminiMCPServer {
  constructor() {
    this.tools = [{
      name: "ask_gemini",
      description: "Ask Gemini AI a question",
      inputSchema: {
        type: "object",
        properties: {
          query: {
            type: "string",
            description: "The question to ask Gemini"
          }
        },
        required: ["query"]
      }
    }];
  }

  async handleRequest(request) {
    const { method, params = {}, id } = request;

    try {
      switch (method) {
        case 'notifications/initialized':
          // This is a notification, no response needed
          return null;
        case 'initialize':
          return {
            jsonrpc: "2.0",
            id,
            result: {
              protocolVersion: "2025-06-18",
              capabilities: {
                tools: {}
              },
              serverInfo: {
                name: "gemini-mcp-server",
                version: "1.0.0"
              }
            }
          };

        case 'tools/list':
          return {
            jsonrpc: "2.0",
            id,
            result: {
              tools: this.tools
            }
          };

        case 'resources/list':
          return {
            jsonrpc: "2.0",
            id,
            result: {
              resources: []
            }
          };

        case 'prompts/list':
          return {
            jsonrpc: "2.0",
            id,
            result: {
              prompts: []
            }
          };

        case 'tools/call':
          const { name, arguments: args } = params;
          if (name === 'ask_gemini') {
            const result = await this.callGemini(args.query);
            return {
              jsonrpc: "2.0",
              id,
              result: {
                content: [
                  {
                    type: "text",
                    text: result
                  }
                ]
              }
            };
          }
          throw new Error(`Unknown tool: ${name}`);

        default:
          throw new Error(`Unknown method: ${method}`);
      }
    } catch (error) {
      return {
        jsonrpc: "2.0",
        id,
        error: {
          code: -32603,
          message: error.message
        }
      };
    }
  }

  async callGemini(query) {
    return new Promise((resolve, reject) => {
      const postData = JSON.stringify({ query });
      
      const options = {
        hostname: '127.0.0.1',
        port: 8811,
        path: '/mcp',
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Content-Length': Buffer.byteLength(postData)
        }
      };

      const req = http.request(options, (res) => {
        let data = '';
        res.on('data', (chunk) => data += chunk);
        res.on('end', () => {
          try {
            const response = JSON.parse(data);
            resolve(response.response || response.text || data);
          } catch (e) {
            resolve(data);
          }
        });
      });

      req.on('error', (error) => {
        console.error('Error calling Gemini server:', error);
        reject(error);
      });
      req.write(postData);
      req.end();
    });
  }
}

// Main execution
const server = new GeminiMCPServer();

process.stdin.setEncoding('utf8');
let buffer = '';

process.stdin.on('data', async (chunk) => {
  buffer += chunk;
  
  // Process complete lines
  let lines = buffer.split('\n');
  buffer = lines.pop(); // Keep incomplete line in buffer
  
  for (const line of lines) {
    if (line.trim()) {
      try {
        const request = JSON.parse(line);
        const response = await server.handleRequest(request);
        if (response !== null) {
          console.log(JSON.stringify(response));
        }
      } catch (error) {
        console.log(JSON.stringify({
          jsonrpc: "2.0",
          id: null,
          error: {
            code: -32700,
            message: "Parse error"
          }
        }));
      }
    }
  }
});

process.stdin.on('end', () => {
  process.exit(0);
});