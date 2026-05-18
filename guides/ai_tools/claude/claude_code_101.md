- CLAUDE.md
- Use when u want something to load in every context.
- eg. use Typescript for all code snippets, follow company code standards, etc.
- User level, for developer.
- Project level, for entire project.
- Location : project root or .claude/CLAUDE.md

- SKILL.md
- Claude auto loads it when required.
eg. commit message format, pr review checklist, etc.
- Location : project root or .claude/skills/skill_name

- Create Agents
- We can create agents in claude code and use them in any context.
- eg. create a research agent that can fetch data from the web, analyze it.

- MCP server
- open standard to connect external tools and APIs.
- Lot of context is outside, eg. DB, public repos.
- eg. If team is using slack, we use Slack MCP.
- MCP is collection of tools.
- Tool is a single API, eg. Slack API for sending messages.
- MCP is collection of tools, eg. Slack MCP is collection of Slack APIs.
- add mcp server via /mcp add MCP server command in claude code.
- manage with /mcp list, /mcp remove, etc.
- Disable what is not used, as mcp adds all tool definition in context.
- Use skills if alternative to mcp, as skills are loaded on demand and do not add all tool definitions in context.

- Hooks:
- U want to run every time something, dont put it in prompt or CLAUDE.md, use hooks.
- Hooks always run.
- eg. run prettier every time, if we give CLAUDE.md not everytime run. Hooks always run.
- eg. logging, compliance checks like avoid modifying production data, etc.
- configured in settings.json file.
```json
{
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Write|Edit|MultiEdit",
        "hooks": [
          {
            "type": "command",
            "command": "prettier --write $CLAUDE_TOOL_INPUT_FILE_PATH"
          },
          {
            "type": "command",
            "command": "echo \"[LOG] $(date) - Modified: $CLAUDE_TOOL_INPUT_FILE_PATH\" >> ~/.claude/activity.log"
          }
        ]
      }
    ],
    "PreToolUse": [
      {
        "matcher": "Write|Edit|MultiEdit",
        "hooks": [
          {
            "type": "command",
            "command": "python3 -c \"\nimport json, sys, os\npath = os.environ.get('CLAUDE_TOOL_INPUT_FILE_PATH', '')\nif 'prod' in path or 'production' in path:\n    print('BLOCKED: Cannot modify production files')\n    sys.exit(2)\n\""
          }
        ]
      }
    ]
  }
}
```

- it uses exit codes to block or pass.
- 0 for pass, 2 for block.
- Push to repo so team can use same hooks.
- Use PreToolUse to block any unwanted action before it happens, and PostToolUse for any follow up action after the tool is used like logging, formatting, etc. 
