---
active: true
iteration: 1
max_iterations: 20
completion_promise: "DONE"
started_at: "2026-02-06T08:51:48Z"
---

Make the cooking-agent stream updates while cooking using TDD.

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
- The cooking agent should now stream updates to the client while cooking.
- Do not change the current agent logic or behavior, just stream updates back to the caller.
- These updates should include the action that the agent is doing, like reserving and over, waiting for an oven or getting ingredients.


Output <promise>DONE</promise> when all tests green.
