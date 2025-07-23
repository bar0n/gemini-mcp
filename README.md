# Gemini MCP Server

A Model Context Protocol (MCP) server that integrates Google's Gemini AI with Claude Desktop, allowing you to ask Gemini questions directly from Claude.

## Architecture

```
Claude Desktop → mcp-bridge.js → Java Spring Boot Server → Gemini API
```

The system consists of:
- **Java Spring Boot Server**: Handles Gemini API calls on port 8811
- **Node.js MCP Bridge**: Translates between MCP protocol and HTTP calls
- **Claude Desktop Integration**: Provides the `ask_gemini` tool

## Prerequisites

- Java 24+
- Node.js 14+
- Docker (optional)
- Claude Desktop
- Google Gemini API key

## Setup

### 1. Get Gemini API Key

1. Visit [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Create a new API key
3. Export it as an environment variable:

```bash
export GEMINI_API_KEY=your_api_key_here
```

### 2. Build and Run

#### Option A: Docker (Recommended)

```bash
# Build the Docker image
./mvnw spring-boot:build-image 

# Run the container with default model (gemini-1.5-flash)
docker run -d --name gemini-mcp -p 8811:8811 -e GEMINI_API_KEY=$GEMINI_API_KEY gemini-mcp:0.0.1-SNAPSHOT

# Or specify a different Gemini model
docker run -d --name gemini-mcp -p 8811:8811 \
  -e GEMINI_API_KEY=$GEMINI_API_KEY \
  -e GEMINI_MODEL=gemini-1.5-pro \
  gemini-mcp:0.0.1-SNAPSHOT
```

#### Option B: Local Development

```bash
# Build with Maven
./mvnw clean package

# Run the JAR
java -jar target/gemini-mcp-0.0.1-SNAPSHOT.jar
```

### 3. Configure Claude Desktop

1. Locate your Claude Desktop MCP configuration file:
   - **macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
   - **Windows**: `%APPDATA%/Claude/claude_desktop_config.json`

2. Add the following configuration:

```json
{
  "mcpServers": {
    "gemini": {
      "command": "node",
      "args": ["mcp-bridge.js"],
      "cwd": "/path/to/your/gemini-mcp/project"
    }
  }
}
```

**Important**: Replace `/path/to/your/gemini-mcp/project` with the actual path to this project directory.

3. Restart Claude Desktop

### 4. Verify Installation

1. Open Claude Desktop
2. Look for the 🔧 tool icon in the interface
3. You should see an `ask_gemini` tool available
4. Test it by asking: "Use ask_gemini to tell me a joke"

## Usage

Once configured, you can use the `ask_gemini` tool in Claude Desktop:

```
Please use ask_gemini to explain quantum computing in simple terms
```

```
Can you ask_gemini what the weather is like today?
```

## Configuration

### Available Models

You can configure which Gemini model to use by setting the `GEMINI_MODEL` environment variable:

- `gemini-1.5-flash` (default) - Fast responses, good for most tasks
- `gemini-1.5-pro` - More capable, better for complex tasks
- `gemini-1.0-pro` - Original Gemini model

### Environment Variables

- `GEMINI_API_KEY` (required): Your Google Gemini API key
- `GEMINI_MODEL` (optional): Gemini model to use (defaults to `gemini-1.5-flash`)

### Configuration Files

- `claude-desktop-mcp.json`: Example MCP server configuration
- `application.properties`: Spring Boot configuration with default model
- `mcp-bridge.js`: Node.js MCP protocol bridge

## Troubleshooting

### Common Issues

**Tool not appearing in Claude Desktop:**
- Check that the `cwd` path in your MCP configuration is correct
- Ensure `mcp-bridge.js` is executable
- Restart Claude Desktop after configuration changes

**Connection errors:**
- Verify the Java server is running on port 8811
- Check Docker container logs: `docker logs gemini-mcp`
- Ensure your Gemini API key is valid and exported

**API errors:**
- Verify your Gemini API key is correct
- Check API quotas and limits in Google AI Studio
- Review server logs for detailed error messages

### Debugging

View container logs:
```bash
docker logs -f gemini-mcp
```

Test the HTTP endpoint directly:
```bash
curl -X POST http://localhost:8811/mcp \
  -H "Content-Type: application/json" \
  -d '{"query": "Hello, Gemini!"}'
```

## Development

The project structure:
```
├── src/main/java/com/baron/geminimcp/
│   ├── GeminiMcpApplication.java      # Spring Boot main class
│   ├── GeminiMcpController.java       # REST controller
│   └── GeminiResponse.java            # Response model
├── mcp-bridge.js                      # MCP protocol bridge
├── claude-desktop-mcp.json           # MCP configuration example
└── docker-compose.yml               # Docker Compose setup
```

## License

MIT License
