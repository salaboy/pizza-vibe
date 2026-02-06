---
active: true
iteration: 1
max_iterations: 20
completion_promise: "DONE"
started_at: "2026-02-04T11:24:33Z"
---

Improve the cooking-agent use of the oven service using TDD.

Process:
1. Write failing test for next requirement
2. Implement minimal code to pass
3. Run tests
4. If failing, fix and retry
5. Refactor if needed
6. Repeat for all requirements

Requirements:
- Look at the code in agents/cooking-agent
- Always look for references:
    - https://quarkus.io/quarkus-workshop-langchain4j/
    - https://quarkus.io/guides/langchain4j
- The oven tool used by the agent has been upgraded, now the cooking agent needs to send the name cooking-agent-joe to reserve an oven.
- Once the pizza is being cooked in an oven the agent needs to check every second if the oven where the pizza is being cooked is released. Once the oven is released the pizza has been cooked. 
- The agent should interact with the  oven service using the Pizza MCP server. Look at the agents/pizza-mcp to register the right tools.

Output <promise>DONE</promise> when all tests green.
