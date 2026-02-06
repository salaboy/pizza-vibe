---
active: true
iteration: 1
max_iterations: 20
completion_promise: "DONE"
started_at: "2026-02-06T09:10:20Z"
---

Change the kitchen service to connect with cooking agent using TDD.

Process:
1. Write failing test for next requirement
2. Implement minimal code to pass
3. Run tests
4. If failing, fix and retry
5. Refactor if needed
6. Repeat for all requirements

Requirements:
- Look for the code in the kitchen/ directory
- Call the cooking agent service to the cook/stream endpoint
- Make changes in code and tests to get the cooking agent stream of updates and send them to the store
- For each pizza cooked by the agent, emit an event for every streamed update from the cooking agent.
- When all the pizzas are cooked still send the DONE event

Output <promise>DONE</promise> when all tests green.
