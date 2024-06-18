package graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class ShortestPathsTest {
    /** The graph example from Prof. Myers's notes. There are 7 vertices labeled a-g, as
     *  described by vertices1. 
     *  Edges are specified by edges1 as triples of the form {src, dest, weight}
     *  where src and dest are the indices of the source and destination
     *  vertices in vertices1. For example, there is an edge from a to d with
     *  weight 15.
     */
    static final String[] vertices1 = { "a", "b", "c", "d", "e", "f", "g" };
    static final int[][] edges1 = {
        {0, 1, 9}, {0, 2, 14}, {0, 3, 15},
        {1, 4, 23},
        {2, 4, 17}, {2, 3, 5}, {2, 5, 30},
        {3, 5, 20}, {3, 6, 37},
        {4, 5, 3}, {4, 6, 20},
        {5, 6, 16}
    };
    static class TestGraph implements WeightedDigraph<String, int[]> {
        int[][] edges;
        String[] vertices;
        Map<String, Set<int[]>> outgoing;

        TestGraph(String[] vertices, int[][] edges) {
            this.vertices = vertices;
            this.edges = edges;
            this.outgoing = new HashMap<>();
            for (String v : vertices) {
                outgoing.put(v, new HashSet<>());
            }
            for (int[] edge : edges) {
                outgoing.get(vertices[edge[0]]).add(edge);
            }
        }
        public Iterable<int[]> outgoingEdges(String vertex) { return outgoing.get(vertex); }
        public String source(int[] edge) { return vertices[edge[0]]; }
        public String dest(int[] edge) { return vertices[edge[1]]; }
        public double weight(int[] edge) { return edge[2]; }
    }
    static TestGraph testGraph1() {
        return new TestGraph(vertices1, edges1);
    }

    @Test
    void lectureNotesTest() {
        TestGraph graph = testGraph1();
        ShortestPaths<String, int[]> ssp = new ShortestPaths<>(graph);
        ssp.singleSourceDistances("a");
        assertEquals(50, ssp.getDistance("g"));
        StringBuilder sb = new StringBuilder();
        sb.append("best path:");
        for (int[] e : ssp.bestPath("g")) {
            sb.append(" " + vertices1[e[0]]);
        }
        sb.append(" g");
        assertEquals("best path: a c e f g", sb.toString());
    }

    @Test
    void edgeCaseTest1() {
        // Create a graph with two vertices and a single edge
        String[] vertices = { "S", "T" };
        int[][] edges = {
                {0, 1, 3} // Edge from S to T with weight 3
        };

        TestGraph graph = new TestGraph(vertices, edges);

        // Run Dijkstra's algorithm from vertex "S"
        ShortestPaths<String, int[]> ssp = new ShortestPaths<>(graph);
        ssp.singleSourceDistances("S");

        // Verify distances and best path
        assertEquals(0, ssp.getDistance("S"));
        assertEquals(3, ssp.getDistance("T"));

        StringBuilder sb = new StringBuilder();
        sb.append("best path:");
        for (int[] e : ssp.bestPath("T")) {
            sb.append(" " + vertices[e[0]]);
        }
        sb.append(" T");
        assertEquals("best path: S T", sb.toString());
    }

    @Test
    void edgeCaseTest2() {
        // Create a graph with three vertices forming a triangle
        String[] vertices = { "A", "B", "C" };
        int[][] edges = {
                {0, 1, 1}, // Edge from A to B with weight 1
                {1, 2, 2}, // Edge from B to C with weight 2
                {2, 0, 3}  // Edge from C to A with weight 3
        };

        TestGraph graph = new TestGraph(vertices, edges);

        // Run Dijkstra's algorithm from vertex "A"
        ShortestPaths<String, int[]> ssp = new ShortestPaths<>(graph);
        ssp.singleSourceDistances("A");

        // Verify distances and best path
        assertEquals(0, ssp.getDistance("A"));
        assertEquals(1, ssp.getDistance("B"));
        assertEquals(3, ssp.getDistance("C"));

        StringBuilder sb = new StringBuilder();
        sb.append("best path:");
        for (int[] e : ssp.bestPath("C")) {
            sb.append(" " + vertices[e[0]]);
        }
        sb.append(" C");
        assertEquals("best path: A B C", sb.toString());
    }

    @Test
    void additionalTest1() {
        // Create a graph with additional vertices and edges
        String[] vertices = { "A", "B", "C", "D", "E" };
        int[][] edges = {
                {0, 1, 5}, {0, 2, 10},
                {1, 3, 7},
                {2, 3, 2}, {2, 4, 15},
                {3, 4, 4}
        };
        TestGraph graph = new TestGraph(vertices, edges);

        // Run Dijkstra's algorithm from vertex "A"
        ShortestPaths<String, int[]> ssp = new ShortestPaths<>(graph);
        ssp.singleSourceDistances("A");

        // Verify distances and best path
        assertEquals(0, ssp.getDistance("A"));
        assertEquals(5, ssp.getDistance("B"));
        assertEquals(10, ssp.getDistance("C"));
        assertEquals(12, ssp.getDistance("D"));
        assertEquals(16, ssp.getDistance("E"));

        StringBuilder sb = new StringBuilder();
        sb.append("best path:");
        for (int[] e : ssp.bestPath("E")) {
            sb.append(" " + vertices[e[0]]);
        }
        sb.append(" E");
        //Best Path would either be ABDE or ACDE, either is correct but I think the algo returns the
        //first path because of it's ordering
        assertEquals("best path: A B D E", sb.toString());
    }

    @Test
    void additionalTest2() {
        // Create a graph with a single vertex
        String[] vertices = { "X" };
        int[][] edges = {}; // No edges

        TestGraph graph = new TestGraph(vertices, edges);

        // Run Dijkstra's algorithm from vertex "X"
        ShortestPaths<String, int[]> ssp = new ShortestPaths<>(graph);
        ssp.singleSourceDistances("X");

        // Verify distance and best path (no edges, so distance should be 0)
        assertEquals(0, ssp.getDistance("X"));

        // Best path for the only vertex should be the vertex itself

        //This is an invalid testcase because there is only one vertice and no edges in the graph so
        //of course bestPath cant be called since there are no paths at all!
        //So rather than checking if the size is equal to 0, just check if it throws an exception!
        //Instead of this:
        //assertEquals(0, ssp.bestPath("X").size());
        //Do this:
        assertThrows(AssertionError.class,() -> ssp.bestPath("X"));
    }
}
