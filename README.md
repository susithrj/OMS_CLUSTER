# Achieving Peak Performance: Our Journey to 2x User Handling with Failover Systems

## Summary

In this project, we designed a distributed system with robust failover mechanisms to enhance execution speed by 100%. This solution was crucial to accommodate the rapid growth of customer onboarding, which had increased at a rate of 2x.

## Situation

We received a request from LBSL Brokerage to scale the performance of their system due to rapid user growth. The goal was to handle 20 lakh orders per day within an 18,000-second window, with a peak requirement of 100 trades per second (TPS). To meet these demands, we had to design a system capable of handling increased user concurrency and load.

### Assumptions

1. **First-year number of users**: 12,000
2. **Assumed concurrency**: 20%, which translates to approximately 24,000 concurrent user sessions.
3. **User behavior**:
    - Most users watch price data without frequently trading.
    - Some users frequently navigate to portfolio/account summary widgets.

## Task

Our primary task was to migrate a single-leg application to an application cluster with failover capabilities to handle the increased load and ensure continuous availability.

## How We Started Designing

1. **Identify the Problem and Goals**: We first identified the problem of handling increased user load and the goal of achieving 2x performance improvement.
2. **Understand Requirements**: We assessed the expected workload, scalability requirements, fault tolerance, and performance criteria.

## Action

![image](https://github.com/susithrj/OMS_Cluster/assets/47299475/0550a03a-097c-45c9-ab76-098e844d780c)


To achieve this, we took the following steps:

1. **Migration to Application Clusters**: We migrated the monolithic application to an application cluster to improve reliability and performance.
2. **Cluster Communication with JGroups**: We used JGroups for cluster communication, implementing our business logic through its interface.
3. **Active-Active Clustering System**: We established an active-active clustering system to ensure continuous availability and load balancing.
4. **Load Balancing and Failover**: 
    - Implemented load balancing with failover mechanisms to maintain seamless service even if a node fails.
    - Users logged into one node remained there until execution came from them.
    - Load balancer routed users to OMS nodes in a round-robin fashion.
5. **Routing Logic**:
    - Used customer ID modulo the number of nodes plus one to determine node assignment.
    - Ensured customers logged into each node remained on that node.
    - Executions were processed by the Order Management System (OMS) where the customer was available.
6. **JMeter for Load Testing**: We used JMeter scripts for load testing the WebSocket handler.

### Forward Flow

- The routing logic ensures users are distributed evenly across nodes using a modulo operation on the customer ID.
- Users stay on their assigned nodes, and executions are processed by the OMS associated with the customer.

### Backward Flow

- The primary OMS handles executions. If the primary OMS goes down, the secondary OMS takes over.
- The failover mechanism ensures that the order reply is processed seamlessly by the secondary OMS if the primary is unavailable.

## Implementation Details

### JGroups Configuration

- **Default Methods**:
  - **Heartbeat**: Assigns the primary OMS node with a heartbeat mechanism.
  - **Watchdog**: Registers components and queries which OMS to send requests to using Falcon.

### Services Registry

- Integrated with **Prometheus** and **Grafana** for monitoring.

### Current Implementation

- **DFIX**: Requests are only processed by the primary OMS.

### New Changes

- **Order Identification**: Orders are now processed by the OMS that initiated them.
- **Primary Broadcast**: The primary OMS broadcasts order events to other nodes.
- **Synchronized Map**: We use a synchronized map to track the `clorderid` and `initiatedOMS` to ensure correct order processing.

### Event Handling

1. **OMS Node Connect/Disconnect**:
   - Created new sync events to handle connections and disconnections, ensuring the map is updated.
   - Handled through a hashmap (HM1: IP to ID, HMp2: ID to `clorderID`).

### Failover Handling

- If the primary OMS is down, the secondary OMS becomes the primary.
- Order processing falls back to the primary OMS if the initiated OMS is unavailable.

## Testing

- Utilized **JMeter** scripts for WebSocket handler load testing.
- Ensured system stability under expected load conditions (80-100 TPS).

- Behavioural Testing
- Test Cases
- https://docs.google.com/spreadsheets/d/1nCYQmOXrgJD8ESJSnDQ9itLx3OfPCTUqDu1SxZjzPEc/edit#gid=0


## Challenges Faced

1. **Server Allocation and Communication Issues**: Allocating new servers led to issues with JGroups messages not working due to firewall settings. Disabling the firewall resolved this issue.
2. **Mapping Order Execution**: Ensuring that order execution was correctly mapped to the customer who initiated it was a critical challenge we addressed through our routing logic.

## Result

The implemented system was rigorously tested under expected load conditions:

- **Load Test Results**:
  - Target Throughput: 80-100 Transactions Per Second (TPS)

The application demonstrated stable behavior and met the expected load conditions, achieving the desired throughput and supporting the client’s growth objectives.

## Release Notes

- **APPSVR_ID** should be unique according to the nodes.

## Deep Dive

1. **Cache Initialization**: New nodes initiate a cache through the default cache mechanism.
2. **Order Processing Logic**:
   - Orders initiated by a specific OMS are processed by that OMS.
   - If the initiating OMS is down, the primary OMS handles the order.
3. **Node Join/Leave Handling**:
   - Ensured seamless order processing even when nodes join or leave the cluster.
   - Updated and synchronized hashmaps to reflect node status changes.

## Conclusion

This project successfully designed a distributed system with effective failover mechanisms, significantly enhancing execution speed and reliability. The system now supports the client's expanding user base, ensuring smooth operation and customer satisfaction.
