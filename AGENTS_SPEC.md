## Prompt to create Quarkus agent with Langchain4j

/ralph-loop:ralph-loop "Implement a new agent using Quarkus and Langchain4j using TDD.

Process:
1. Write failing test for next requirement
2. Implement minimal code to pass
3. Run tests
4. If failing, fix and retry
5. Refactor if needed
6. Repeat for all requirements

Requirements:
- Create a new Quarkus Agent using Langchain4j by following the documentation located in the following places:
  - https://quarkus.io/quarkus-workshop-langchain4j/
  - https://quarkus.io/guides/langchain4j
- The agent should be called cooking-agent inside the agents/ folder. 
- Use Maven to create the project.
- The agent should have the goal to cook pizzas based on ingredients available in the inventory.
- The agent should have an internal inventory of ingredients with mock data.
- If the agent has enough ingredients to cook pizzas, it should return a list of pizzas that were cooked.
- The agent should expose a REST endpoint to cook pizza orders.

Output <promise>DONE</promise> when all tests green." --max-iterations 20 --completion-promise "DONE"


## Create MCP Server using Quarkus and register tools for inventory and oven services

/ralph-loop:ralph-loop "Create a new MCP Server using Quarkus and register inventory and oven services as MCP tools using TDD.

Process:
1. Write failing test for next requirement
2. Implement minimal code to pass
3. Run tests
4. If failing, fix and retry
5. Refactor if needed
6. Repeat for all requirements

Requirements:
- Create Pizza MCP server inside the agents/pizza-mcp directory.
- Always look for references:
    - https://quarkus.io/quarkus-workshop-langchain4j/
    - https://quarkus.io/guides/langchain4j
- The MPC server should register the inventory and oven services as MPC tools.
- The MCP server should be a standalone MCP server and follow https://quarkus.io/quarkus-workshop-langchain4j/section-1/step-08/

Output <promise>DONE</promise> when all tests green." --max-iterations 20 --completion-promise "DONE"

## Prompt to refine Quarkus cooking agent with Langchain4j

/ralph-loop:ralph-loop "Improve the cooking-agent using TDD.

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
- The agent should now use the ingredients from the inventory service instead of mock data.
    - - Look for the go data model and data types inside the inventory service located in the inventory/ directory.
- The agent should now also use the oven service to cook pizzas.
- The agent should use the @Agent annotation from Quarkus Langchain4j integration.
- The agent should interact with the inventory and oven services using the Pizza MCP server. Look at the agents/pizza-mcp to register the right tools.
- The agent should have a Dockerfile to build and run the agent.

Output <promise>DONE</promise> when all tests green." --max-iterations 20 --completion-promise "DONE"


## Prompt to update the mcp server with new oven behavior

/ralph-loop:ralph-loop "Update the Pizza MCP server with new oven tools using TDD.

Process:
1. Write failing test for next requirement
2. Implement minimal code to pass
3. Run tests
4. If failing, fix and retry
5. Refactor if needed
6. Repeat for all requirements

Requirements:
- Look at the code in agents/pizza-mcp
- The oven service located in /oven has been update, look at the code and update the MPC tools created for that service based on the new code

Output <promise>DONE</promise> when all tests green." --max-iterations 20 --completion-promise "DONE"


## Prompt to refine cooking agent with new oven behavior

/ralph-loop:ralph-loop "Improve the cooking-agent use of the oven service using TDD.

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
- The oven tool used by the agent has been upgraded, now the cooking agent needs to send the name "cooking-agent-joe" to reserve an oven.
- Once the pizza is being cooked in an oven the agent needs to check every second if the oven where the pizza is being cooked is released. Once the oven is released the pizza has been cooked. 
- The agent should interact with the  oven service using the Pizza MCP server. Look at the agents/pizza-mcp to register the right tools.

Output <promise>DONE</promise> when all tests green." --max-iterations 20 --completion-promise "DONE"


## Prompt to refine cooking agent streaming updates behavior

/ralph-loop:ralph-loop "Make the cooking-agent stream updates while cooking using TDD.

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


Output <promise>DONE</promise> when all tests green." --max-iterations 20 --completion-promise "DONE"

## Create a new store management agent

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
- Create a new store-mgmt-agent directory inside the agents/ directory to contain all the agent code.
- Using the quarkus @Sequential annotation, the store management agent should use the cooking-agent and delivery agent to manager store orders.
- The store management agent should expose a REST endpoint to manage store orders.


Output <promise>DONE</promise> when all tests green.
