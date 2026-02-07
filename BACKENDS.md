## Store Service

/ralph-loop:ralph-loop "Implement store service in Go using TDD.

Process:
1. Write failing test for next requirement
2. Implement minimal code to pass
3. Run tests
4. If failing, fix and retry
5. Refactor if needed
6. Repeat for all requirements

Requirements:
1. Create a new directory called store and place all store service files inside
2. The store service should expose the following endpoints:
    1. POST endpoint /order to place a pizza order
    2. POST endpoint /events to receive updates from the kitchen and delivery services
    3. Create a websocket connection to the frontend application to send order updates
3. The order data model should include orderId(UUID), OrderItems, orderData and orderStatus
    1. OrderItems must container the pizzaType and the number of pizzas requested for that type
4. Use Go Chi for REST endpoints
5. Document all code and progress

Output <promise>DONE</promise> when all tests green." --max-iterations 25 --completion-promise "DONE"


## Kitchen Service

/ralph-loop:ralph-loop "Implement kitchen service in Go using TDD.

Process:
1. Write failing test for next requirement
2. Implement minimal code to pass
3. Run tests
4. If failing, fix and retry
5. Refactor if needed
6. Repeat for all requirements

Requirements:
1. Create a new directory called kitchen and place all kitchen service files inside
2. The kitchen service should expose the following endpoints:
    1. POST endpoint /cook to cook the OrderItems from the store order
    2. The payload should only be the orderId and orderItems
    3. For each orderitem it should take a random time from 1 to 10 seconds to cook the item. Each item cooked should be printed in the terminal with the amount that it took to be printed
3. Use Go Chi for REST endpoints
4. Document all code and progress
5. Create a docker-compose file to run all services of the application in the root directory
6. Add instruction on how to run all the services in the README.md file at the root directory

Output <promise>DONE</promise> when all tests green." --max-iterations 25 --completion-promise "DONE"


## Implement Kitchen Interaction Feature in Store

/ralph-loop:ralph-loop "Implement calling kitchen and exchange events using TDD.

Process:
1. Write failing test for next requirement
2. Implement minimal code to pass
3. Run tests
4. If failing, fix and retry
5. Refactor if needed
6. Repeat for all requirements

Requirements: 
- For the store service: 
  - Return the Order ID so it can be used to track the order in the frontend application
  - Call the kitchen service to cook the order when the order is placed passing the orderId and orderItems
  - Accept update and Done events from the kitchen service to track the order status. Keep track of events per orderId
  - When a done event is received, update the order status to COOKED
- For the kitchen service: 
  - Print the amount of time it took to cook each order item
  - Send update events to the store service every second while the order is cooking. Events are sent using HTTP to the the store service /events endpoint
  - Send a DONE event when the order is done cooking

Output <promise>DONE</promise> when all tests green." --max-iterations 20 --completion-promise "DONE"


## Implement Event and Order List in Management Page Feature in Store

/ralph-loop:ralph-loop "Implement list events and list orders in management page using TDD.

Process:
1. Write failing test for next requirement
2. Implement minimal code to pass
3. Run tests
4. If failing, fix and retry
5. Refactor if needed
6. Repeat for all requirements

Requirements:
- For the store service:
    - Make sure that there is an endpoint that returns all the orders (GET /orders)
    - Make sure that there is an endpoint that returns all events per order (GET /events)
- For front-end
    - in the management page consume both endpoints to list all the orders and their status
    - all the events per order, when the order is selected.

Output <promise>DONE</promise> when all tests green." --max-iterations 20 --completion-promise "DONE"


## Implement websockets event exchange between store and frontend

/ralph-loop:ralph-loop "Implement websocket events between store and frontend using TDD.

Process:
1. Write failing test for next requirement
2. Implement minimal code to pass
3. Run tests
4. If failing, fix and retry
5. Refactor if needed
6. Repeat for all requirements

Requirements:
- For the store service:
    - Everytime that an event is received to the /events endpoint, send it to the frontend using websockets
    - Create the websocket event format as a go type and send it using websockets
    - Create a websocket connection using the client id
- For front-end
    - In the main page, when the order is placed by the user subscribe to the websocket events and display the incoming websocket events related to the order in the UI
    - When the order is placed display the returning order id
    - Add a websocket connection indicator in the UI
    - When connecting to the websocket create a unique client id, this client ide will be used to subscribe to the websocket events
    - Use a table format to display the events related to the order

Output <promise>DONE</promise> when all tests green." --max-iterations 20 --completion-promise "DONE"


## Delivery Service

/ralph-loop:ralph-loop "Implement delivery service in Go using TDD.

Process:
1. Write failing test for next requirement
2. Implement minimal code to pass
3. Run tests
4. If failing, fix and retry
5. Refactor if needed
6. Repeat for all requirements

Requirements:
1. Create a new directory called delivery and place all delivery service files inside
2. The delivery service should expose the following endpoints:
    1. POST endpoint /deliver to deliver and order to the customer
    2. The payload should only be the orderId and orderItems
    3. For each order delivery the service should notify the store service sending regular update events about the order delivery process
    4. To simulate the order being delivered use a random time interval between 5 and 20 seconds, and send status updates every second.
    5. The event updates should include which percentage of the delivery process is completed
    6. When the delivery is done, send a DELIVERED event to the store service
3. Use Go Chi for REST endpoints
4. Document all code and progress
5. Add the kitchen service to the docker-compose.yaml file that already exists in the root directory
6. Add instruction on how to run all the services in the README.md file at the root directory

Output <promise>DONE</promise> when all tests green." --max-iterations 25 --completion-promise "DONE"


## Implement Delivery Interaction Feature in Store

/ralph-loop:ralph-loop "Implement calling delivery and exchange events using TDD.

Process:
1. Write failing test for next requirement
2. Implement minimal code to pass
3. Run tests
4. If failing, fix and retry
5. Refactor if needed
6. Repeat for all requirements

Requirements:
- For the store service:
    - After the order is cooked by the kitchen service, call the delivery service to deliver the order to the customer
    - Call the delivery service to deliver the order after the order COOKED passing the orderId and orderItems
    - Accept update and Delivered events from the delivery service to track the order status. Keep track of events per orderId
    - When a done event is received, update the order status to DELIVERED
- For the delivery service:
    - Print the amount of time it took to delivery the order 
    - Send update events to the store service every second while the order is being delivered. Events are sent using HTTP to  the store service /events endpoint
    - Send a DELIVERED event when the order is delivered

Output <promise>DONE</promise> when all tests green." --max-iterations 20 --completion-promise "DONE"



## Implement Pizzas Order Cart in Store

/ralph-loop:ralph-loop "Implement pizza cart functionality in the frontend using TDD.

Process:
1. Write failing test for next requirement
2. Implement minimal code to pass
3. Run tests
4. If failing, fix and retry
5. Refactor if needed
6. Repeat for all requirements

Requirements:
- Create cart functionality in the frontend application to add, remove and update the pizza items added to the order
- The front end should enable the user to select multiple pizza types and quantity and add that to the order
- Follow the data model defined in the store service for the Order and OrderItems 

Output <promise>DONE</promise> when all tests green." --max-iterations 20 --completion-promise "DONE"


## Implement Inventory Service 

/ralph-loop:ralph-loop "Implement inventory service using TDD.

Process:
1. Write failing test for next requirement
2. Implement minimal code to pass
3. Run tests
4. If failing, fix and retry
5. Refactor if needed
6. Repeat for all requirements

Requirements:
- Create inventory directory and place all inventory service files inside
- The inventory service should expose the following endpoints:
    - GET endpoint /inventory to return all the available items in the inventory
    - GET endpoint /inventory/{item} to return the quantity of the item in the inventory
    - POST endpoint /inventory/{item} to get one item from the inventory, this will decrease by one the quantity of that item
      - If the quantity is 0, return an EMPTY
      - If the quantity is not 0, return the ACQUIRED
- Use Go Chi for REST endpoints
- Document all code and progress
- The inventory service should return the following inventory:
    - Pepperoni: 10
    - Pineapple: 10
    - PizzaDough: 10
    - Mozzarella: 10
    - Sauce: 10

Output <promise>DONE</promise> when all tests green." --max-iterations 20 --completion-promise "DONE"


## Implement Oven Service

/ralph-loop:ralph-loop "Implement oven service using TDD.

Process:
1. Write failing test for next requirement
2. Implement minimal code to pass
3. Run tests
4. If failing, fix and retry
5. Refactor if needed
6. Repeat for all requirements

Requirements:
- Create oven directory and place all oven service files inside
- The oven service represents a set of available ovens that can be used to cook the pizza items.
- To cook in an oven, the user needs to aquire the oven by id and lock it
- The oven service should expose the following endpoints:
    - GET endpoint /ovens/ to return all the available ovens and their status
    - GET endpoint /ovens/{ovenId} return the status of the oven
    - POST endpoint /ovens/{ovenId} reserves and oven
    - DELETE endpoint /ovens/{ovenId} releases the oven
- To reserve an oven, the oven must be available and the user should be sent as a parameter
- Each time that an operation is executed on an oven, the oven status should be updated and a timestamp should be recorded
- The oven service should return the following oven status:
    - AVAILABLE: the oven is available to be reserved
    - RESERVED: the oven is reserved by the user
- Use Go Chi for REST endpoints
- Document all code and progress
- Initially there should be 4 ovens available

Output <promise>DONE</promise> when all tests green." --max-iterations 20 --completion-promise "DONE"


## Implement basic pages for Oven and inventory Services

/ralph-loop:ralph-loop "Implement basic pages for oven and inventory services on the frontend using TDD.

Process:
1. Write failing test for next requirement
2. Implement minimal code to pass
3. Run tests
4. If failing, fix and retry
5. Refactor if needed
6. Repeat for all requirements

Requirements:
- Create a basic page for the oven service that shows the status of the ovens and allows the user to reserve and release an oven
- Create a basic page for the inventory service that shows the inventory items and allows the user to acquire an item
    - Implement the backend logic to add more quantity to the items in the inventory
    - Implement a section in the inventory page to add more quantity to the items

Output <promise>DONE</promise> when all tests green." --max-iterations 20 --completion-promise "DONE"

## Improve oven service

/ralph-loop:ralph-loop "Change the oven service behavior to lock oven based on time TDD.

Process:
1. Write failing test for next requirement
2. Implement minimal code to pass
3. Run tests
4. If failing, fix and retry
5. Refactor if needed
6. Repeat for all requirements

Requirements:
- Look for the code in the oven/ directory
- Make changes in code and tests to accept the username that is reserved the oven, make sure that this is included in the data model
- When an oven is reserved it will remain reserved for a random amount of time between 5 and 20 seconds and then it will be released
- Remove the endpoint to manually release an oven

Output <promise>DONE</promise> when all tests green." --max-iterations 20 --completion-promise "DONE"


## Connect Kitchen Service with Cooking Agent

/ralph-loop:ralph-loop "Change the kitchen service to connect with cooking agent using TDD.

Process:
1. Write failing test for next requirement
2. Implement minimal code to pass
3. Run tests
4. If failing, fix and retry
5. Refactor if needed
6. Repeat for all requirements

Requirements:
- Look for the code in the kitchen/ directory
- Make changes in code and tests to send a request to the cooking-agent for every pizza in the order
  - Remove the logic to wait for every pizza in the order.
  - Check the agents/cooking-agent directory to understand the data types for the request
  - For each pizza cooked by the agent, emit an event to the store service
  - When all the pizzas are cooked still send the DONE event 

Output <promise>DONE</promise> when all tests green." --max-iterations 20 --completion-promise "DONE"


## The Kitchen Service should receive Cooking Agent stream of updates and send them to the store

/ralph-loop:ralph-loop "Change the kitchen service to connect with cooking agent using TDD.

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

Output <promise>DONE</promise> when all tests green." --max-iterations 20 --completion-promise "DONE"

## The Frontend should show all the cooking updates received from the kitchen service 

/ralph-loop:ralph-loop "Review the frontend data types to show updates coming from the kitchen using TDD.

Process:
1. Write failing test for next requirement
2. Implement minimal code to pass
3. Run tests
4. If failing, fix and retry
5. Refactor if needed
6. Repeat for all requirements

Requirements:
- Look for the code in the kitchen/ directory
- Check the data types for the cooking update events and make sure that those are included in the frontend data model
- Show the cooking updates in the frontend, including the parameters that were used
- Avoid showing "tool_completed" without context
- In the frontend, show the percentage of the cooking process completed
- In the frontend, when an order is created keep it in the session storage, so the user can change pages and come back to the order
  - Also keep the websocket open even when the user changes pages

Output <promise>DONE</promise> when all tests green." --max-iterations 20 --completion-promise "DONE"



## The Bikes service

/ralph-loop:ralph-loop "Create the bikes service to reserve bikes using TDD.

Process:
1. Write failing test for next requirement
2. Implement minimal code to pass
3. Run tests
4. If failing, fix and retry
5. Refactor if needed
6. Repeat for all requirements

Requirements:
- Following the structure of the inventory service, create a bikes service to reserve bikes
- The bikes service should expose the following endpoints:
    - GET endpoint /bikes to return all the available bikes and their status
    - GET endpoint /bikes/{bikeId} return the status of the bike
    - POST endpoint /bikes/{bikeId} reserves and bike
- To reserve a bike, the bike must be available and the user should be sent as a parameter
- Each time that an operation is executed on a bike, the bike status should be updated and a timestamp should be recorded
- The bikes service should return the following bike status:
    - AVAILABLE: the bike is available to be reserved
- When the bike is reserved it will automatically free itself after a random amount of time between 10 and 20 seconds
- While the bike is reserved it should emit events to the store service reporting that the bike is on route to deliver a pizza order


Output <promise>DONE</promise> when all tests green." --max-iterations 20 --completion-promise "DONE"