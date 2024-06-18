# MazeNavigation

## Problem Overview

In this assignment, we were tasked with modeling and navigating the London Sewer System, represented as a graph. The protagonist, McDiver, needs to find a special ring hidden in the sewer and then collect treasure while navigating to the exit within a prescribed number of steps.

![a6-fig1](https://github.com/vvnu0/MazeNavigation/assets/76607707/81919c0c-8f96-4b28-8526-84a827408266)
_Finding the special ring_

![a6-fig2](https://github.com/vvnu0/MazeNavigation/assets/76607707/bfe9eac2-eeb4-4974-a10d-c780975cbea5)
_Collecting the max amount of treasure_


## Learning Objectives
- Implement and optimize graph traversal algorithms.
- Manage and synchronize concurrent tasks in Java.
- Enhance algorithm efficiency using advanced data structures like heaps.
## Features
- _Graph Navigation_: Implement Dijkstra’s algorithm to find the shortest paths within the sewer system.
- _Concurrency Management_: Efficient synchronization in a multi-threaded environment to manage GUI animations and logical computations effectively.
- _Treasure Collection Simulation_: After finding the ring, collect as much treasure as possible while navigating to the exit.
- _Performance Optimization_: Use of efficient data structures and algorithms to enhance performance.

## Part 1: Dijkstra's Algorithm

### Implementation

I implemented Dijkstra's single-source shortest-paths algorithm in the `graph.ShortestPaths` class. The algorithm was implemented in a generic way, allowing it to be reused in different parts of the program with different types of vertices and nodes.

The `WeightedDigraph` interface and its parent `DirectedGraph` provided the necessary methods to navigate the graph. I utilized a priority queue to efficiently select the next node to process during the algorithm's execution.

### Testing

To ensure the correctness of my Dijkstra's algorithm implementation, I added additional test cases to `tests/graph/ShortestPathsTests.java`. These test cases covered various scenarios and edge cases to verify that the algorithm produced the expected shortest paths and distances.

## Part 2: Navigation

### Ring Phase

In this phase, McDiver explores the sewer system to find the special ring. The layout of the sewer system is initially unknown, and McDiver only has information about the current location, immediate neighbors, and the Manhattan distance to the ring.

I implemented the seek phase in the `diver.McDiver` class. My approach involved using a depth-first search (DFS) to traverse the graph and find the ring. I organized the traversal in separate well-specified methods to ensure flexibility and maintainability.

To optimize the path to the ring, I employed heuristics based on the Manhattan distance to guide the search towards promising directions. This helped reduce the number of steps taken beyond the minimum possible path.

### Treasure Phase _(return to exit)_

** _a reminder of the algorithm used to solve Sudoku -- brute force + pruning
In this phase, McDiver must reach the exit of the sewer system within a prescribed number of steps while collecting as much treasure as possible along the way. McDiver now has access to a complete map of the sewer system._

I implemented the scram phase in the `diver.McDiver` class as well. Our strategy for the scram phase utilizes the idea of pruning. Specifically, it is a depth-first search (DFS) that attempts to find the maximal path between as many coin nodes as possible, ending with the exit node. Since computing all these combinations would take a factorial amount of time, I use pruning to significantly reduce the number of unnecessary recursion steps.

Initially, we tried to use dynamic programming (DP) to maximize the coin value for the scram method. We assumed that all the edges were of equal weight and found the best path to collect all the coins. However, upon realizing that the edges had different weights, we had to update our code to account for that, which significantly increased the processing time.

Even with pruning, finding the actual optimal solution within the time constraints proved to be challenging. As a result, we cut off the recursion calls after around 9.9 seconds to ensure that McDiver escapes the sewer system in time. Pruning guarantees that all the generated solutions will end with McDiver escaping; if he couldn't escape, the path would be pruned.

To further optimize the pruning process, we implemented a greedy sorting mechanism within the `allPaths` map. The paths are stored in a sorted array inside the map, where the sorting is based on the proximity of other nodes to the current node. This additional layer of greediness helps the pruning algorithm find the best path faster, especially when dealing with a large number of coins.

## Part 3: GUI

### Concurrency
To synchronize the concurrent code, we utilized the synchronization primitives provided by Java. Here's how we approached the task:

1. In the gui.GUI class, we added three synchronized methods:
- startAnimating(): This method sets the animating flag to true, indicating that an animation has started.
- isAnimating(): This method returns the current value of the animating flag.
- finishAnimating(): This method sets the animating flag to false and notifies any waiting threads that the animation is complete using notify().
2. In the game.GUIControl class, we modified the waitForAnimation() method to efficiently wait for the animation to complete. Instead of using a spin loop, we used the wait() method inside a synchronized block to release the lock and wait for notification from the GUI thread.

'''
public static void waitForAnimation(Maybe<GUI> guiOpt) {
    guiOpt.thenDo(gui -> {
        synchronized (gui) {
            while (gui.isAnimating()) {
                try {
                    gui.wait(); // Release the lock and wait for notification
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    });
}
'''

## Installation and Usage
Clone the project and navigate to the directory:
'''
git clone https://github.com/yourgithubusername/sewer-navigation.git
cd sewer-navigation
'''

Compile and run the application:
'''
javac -d bin src/*.java
java -cp bin game.Main
'''
- We used the thenDo() method of the Maybe class to safely access the GUI object, if present.
- Inside the synchronized block, we checked the animating flag using gui.isAnimating().
- If an animation is in progress, we called gui.wait() to release the lock and wait for notification from the GUI thread.
- If an InterruptedException occurs, we properly handled it by interrupting the current thread.

3. When the animation is complete, the GUI thread calls the finishAnimating() method, which sets the animating flag to false and notifies any waiting threads using notify().
