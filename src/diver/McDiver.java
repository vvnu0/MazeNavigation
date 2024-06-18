package diver;

import game.*;
import graph.ShortestPaths;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.*;


/** This is the place for your implementation of the {@code SewerDiver}.
 */
public class McDiver implements SewerDiver {


    /** See {@code SewerDriver} for specification. */
    @Override
    public void seek(SeekState state) {
        greedStar(state);
    }

    //This isn't dfs at all! It's greedy!
    //It has the chance to get stuck
    private void dfs(SeekState state) {
        // Base case: McDiver is standing on the ring
        if (state.distanceToRing() == 0) {
            return;
        }

        // Explore neighbors and move to the one with the minimum distance to the ring
        Collection<NodeStatus> neighbors = state.neighbors();
        int minDistance = Integer.MAX_VALUE;
        long nextMove = -1;

        for (NodeStatus neighbor : neighbors) {
            int distanceToRing = neighbor.getDistanceToRing();
            if (distanceToRing < minDistance) {
                minDistance = distanceToRing;
                nextMove = neighbor.getId();
            }
        }

        // Move to the neighbor with the minimum distance to the ring
        state.moveTo(nextMove);

        // Continue exploring
        dfs(state);
    }


    //A* might not actually be optimal here because you are only allow to move to adjacent tiles
    private void greedStar(SeekState state){
        //Custom comparator for A* frontier
        //Priority cost f(n) = g(n) + h(n) where h(n) is an admissible heuristic
        Queue<Entry<Long,Integer>> frontier =
                new PriorityQueue<>(Comparator.comparingInt(Entry::getValue));

        //Hashmap representing all known path's to reach a particular location.
        //This will be a list of lists ensuring that all paths, from best to worst, are visible
        //This allows the 'move' selection phase to be able to select not the BEST path, but the
        //path with the shortest number of extra moves to get from pos to loc!
        Map<Long,List<List<Long>>> paths = new HashMap<>();
        Entry<Long,Integer> pos = new SimpleEntry<>(state.currentLocation(),state.distanceToRing());
        frontier.offer(pos);
        paths.put(pos.getKey(),new ArrayList<>());
        paths.get(pos.getKey()).add(new ArrayList<>());
        while(!frontier.isEmpty()){
            //OK one issue I discovered is that the pQueue deals VERY poorly with duplicate
            //f(n) values. This causes a lot of backtracking to occur... which is very bad!
            //There is not really a 'good' solution to this, but one easy to code
            //however computationally expensive solution would be to simply poll all values with the
            //'same' f(n), and decide to use the one that is 'closest' to loc.
            //The easiest way to do this would be to check if any of the polled values are neighbors
            //with pos, if they are not, disregard them and put them back into the queue, if they
            //are, precede ahead!
            //If there are no neighbors whatsoever, are if there is just a single element polled,
            //precede ahead with the last one.
            //This would work and solve the issue somewhat, but perhaps a smarter solution would be
            //to convert the code that determines the path from pos to loc to a method and call that
            //for every location.
            //Then choose the location that has the minimum distance from pos, be it a neighbor or a
            //few tiles away.
            //This will also work, but is super expensive and might not even be a worthwhile
            //optimization as perhaps the 'closer' locations also lead to getting stuck...
            //As a result, I will attempt the first optimization, and if that doesn't help, I will
            //come back and optimize further
            Entry<Long,Integer> loc = frontier.poll();
            List<Entry<Long,Integer>> ties = new ArrayList<>();
            while(!frontier.isEmpty() && frontier.peek().getValue() == loc.getValue()){
                ties.add(frontier.poll());
            }
            if(!ties.isEmpty()){
                ties.add(loc);
                for(Entry<Long,Integer> candidate: ties){
                    if(checkNeighbor(state,candidate)){
                        loc = candidate;
                        break;
                    }
                }
                //Now put all values except loc back into the pQueue!
                ties.remove(loc);
                for(Entry<Long,Integer> item: ties){
                    frontier.offer(item);
                }
            }
            //Now if the next item in the pQueue happens to contain a neighbor, that will be chosen
            //over non-neighbor items!
            //I hope this at least eases the damage caused by this problem!
            //Now check if loc is a neighbor of pos
            boolean isNeighbor = checkNeighbor(state,loc);


            //If you are not at the neighboring tile, move back to loc
            //This is done by getting the current optimal path used to reach loc, and the current
            //optimal path used to reach pos, and finding the 'furthest' location that occurs in
            //both the pos and loc paths.
            //This will allow McDiver to 'switch' paths from the path to pos to the path to loc.
            //This allows McDiver to save a few moves as he will no longer have to return to the
            //start of the maze, and instead just return to the intersection where he can then go to
            //loc.
            //This has a few drawbacks that can prevent it from being truly 'optimal'
            //such possibilities include a situation where the path to pos had recently been updated
            //to create a new 'optimal' path, but the path to loc had not been updated.
            //As such it would be possible for the new path to require returning all the way to the
            //start before McDiver could start heading to loc, while the old path, having overlap w/
            //loc, would allow McDiver to reach loc sooner, even though the distance from start is
            //further in the old path.
            //Such considerations are worth noting, however many of the solutions to this problem
            //introduce even more of their own, and an optimal way of solving this would require
            //solving an NP problem, which would be very impossible
            //With all this considered, I will still go through with this form of optimization as
            //even in the worst case scenario, this is at least the same as the alternative.
            //GUESS WHAT, I was right, I did need to further optimize it as what I said earlier does
            //occur frequently enough for it to be bad!
            //To deal with this. I will convert my 'moves' section into its own method that simply
            //returns the number of moves. This is something I was considering for a while, but now
            //I think it is time to do it.
            //I will then call findPath in a for-loop for every path at paths.get(loc) and pos
            //This will result in O(n^4), but I think it should still be ok... maybe
            List<Long> locPath = new ArrayList<>();
            if(!pos.equals(loc) && !isNeighbor){
                List<List<Long>> locPaths = paths.get(loc.getKey());
                List<List<Long>> posPaths = paths.get(pos.getKey());
                List<Long> bestMoves = new ArrayList<>();
                List<Long> moves;
                int pathLength = -1;
                //Here is where it becomes O(n^4)
                for(List<Long> posPath: posPaths){
                    for(List<Long> curLocPath: locPaths){
                        moves = findPath(curLocPath,posPath);
                        if(pathLength == -1 || moves.size() < pathLength){
                            pathLength = moves.size();
                            bestMoves = moves;
                            locPath = curLocPath;
                        }
                    }
                }
                moves = bestMoves;


                //now that the path from pos to loc is determined, I can now make the journey there
                for(long id:moves){
                    state.moveTo(id);
                }
                //By the end of this, the location of McDiver should be neighbors with loc, if not
                //an invariant is violated
                assert checkNeighbor(state,loc);
                //The current state of McDiver also should not be equal to loc if it is here!
                assert state.currentLocation() != loc.getKey();
            }else{
                locPath = paths.get(loc.getKey()).get(paths.get(loc.getKey()).size()-1);
            }
            //After running this if statement, it is now safe to assume that either pos == loc, or
            //pos is a neighbor of loc. Either way, we can safely move to loc
            if(!pos.equals(loc)){
                state.moveTo(loc.getKey());
                pos = loc;

            }
            //Now state.currentLocation MUST equal both pos AND loc
            assert state.currentLocation() == loc.getKey();
            assert state.currentLocation() == pos.getKey();
            //WOW, that was a lot of work, but now things get much easier!
            //Now all that is left is to create a simple A*!


            //Before checking for neighboring nodes, I can check to see if loc IS the ring
            if(state.distanceToRing() == 0){
                return;
            }
            //Now check for neighboring nodes!
            for(NodeStatus node: state.neighbors()){
                //Now calculate whether the node has been seen before, and if so, if it is a more
                //efficient route than the previous route. If either of these are true, update paths
                if(!paths.containsKey(node.getId()) ||
                        paths.get(node.getId()).size() > locPath.size() +1){
                    //next is defined as <location, g(n-1) +1 + h(n)
                    //I'M SO STUPID!!! I DON'T WANT TO DO A* AT ALL HERE! I ACTUALLY JUST WANT TO DO
                    //AN OPTIMIZED GREEDY :'(
                    Entry<Long,Integer> next = new SimpleEntry<>(node.getId(),
                           node.getDistanceToRing());
                    //I will have duplicate values in my pQueue as I really don't want to change the
                    //priority during runtime. As such, there is a non-zero possibility that my algo
                    //takes MUCH longer than expected on certain cases where the entire queue is
                    //filled with duplicates. If this is the case, I will revisit this code and
                    //optimize it in order to prevent such a situation to happen. But that process
                    //will be very slow and painful, so I will attempt to put that off unless this
                    //becomes an issue.

                    //Now I also gotta update this part of the method to account for the all paths
                    frontier.offer(next);
                    List<List<Long>> allPaths = paths.getOrDefault(node.getId(),new ArrayList<>());
                    List<Long> path = new ArrayList<>(locPath);
                    path.add(loc.getKey());
                    allPaths.add(path);
                    paths.put(next.getKey(),allPaths);
                }else{
                    List<Long> path = new ArrayList<>(locPath);
                    path.add(loc.getKey());
                    paths.get(node.getId()).add(path);
                }
            }
        }
        //This should work! Though I think I might have to wait until I finish scram before testing.



    }

    //check to see if los is a neighbor of state.getLocation
    private boolean checkNeighbor(SeekState state,Entry<Long,Integer> loc){
        boolean isNeighbor = false;
        for(NodeStatus nodes: state.neighbors()){
            if(nodes.getId() == loc.getKey()){
                return true;
            }
        }
        return isNeighbor;
    }

    //Finds the path between loc and pos and returns it as a list of moves
    private List<Long> findPath(List<Long> locPath, List<Long> posPath){
        //start back tracking through pos and checking for overlap
        //This is O(n^2) as for every n in posPath, I will check with .contains if it is in
        //locPaths.
        //This is probably not too important as my algo is already effectively O(2^n) so
        //this doesn't really matter much all things considered
        //if a switch point isn't found before i=0, then the switch point is the start
        List<Long> moves = new ArrayList<>();
        for(int i = posPath.size()-1; i > 0; i--){
            moves.add(posPath.get(i));
            if(locPath.contains(posPath.get(i))){
                break;
            }

        }
        //now append the moves require to get to pos!
        //System.out.println(locPath + "/" + posPath + "/" + moves);
        for(int i = locPath.indexOf(moves.get(moves.size()-1))+1; i < locPath.size(); i++){
            moves.add(locPath.get(i));
        }
        return moves;
    }
    /** See {@code SewerDriver} for specification. */
    @Override
    public void scram(ScramState state) {
        doScram(state);
    }

    private void doScram(ScramState state){
        //System.out.println(state);
        //System.out.println(state.allNodes());
        //The DP to solve the entire problem :)
        //DP[i][j] represents the max score that can be obtained with i moves used while at node j
        //it is gaurenteed that dp[numMoves-1] will only have 1 non-zero value, and that is j=exit
        //Each value in the dp is a 2-element array
        //The first element is the max score you can obtain, while the second is the last node that
        //I came from!
        //This is the value that is the legitimate max value I can obtain!


        //First calculate the max coin value possible, the length for the dp and create a mapping
        //between node IDs and node objects!

        //Also generate the optimal paths from any given node to the exit node using dijkstra's!
        Map<Long,List<Edge>> paths = new HashMap<>();
        ShortestPaths dijkstra = new ShortestPaths(new Maze((Set<Node>) state.allNodes()));
        dijkstra.singleSourceDistances(state.exit());
        Map<Long,Node> map = new HashMap<>();
        Set<Long> coinIds = new HashSet<>();
        Map<Long,Long> exitDist = new HashMap<>();
        int coins = 0;
        long maxID = 0;
        for(Node node: state.allNodes()){
           if(node.getId() > maxID){
               maxID = node.getId();
           }
           if(node.getTile().coins() > 0){
               coinIds.add(node.getId());
           }
           coins +=node.getTile().originalCoinValue();
           map.put(node.getId(),node);
            List<Edge> nodePath = dijkstra.bestPath(node);
            long weight = 0;
            for(Edge edge: nodePath){
                weight+=edge.length();
            }
            exitDist.put(node.getId(),weight);
           paths.put(node.getId(),nodePath);
        }


        //New Idea! create 2d optimal path array, then try ordering them and find either the first
        //order that can be completed in numMoves times or the best ordering!
        //Using paths will GREATLY reduce the complexity of this process!
        //Furthermore, if I only include paths between coins, the starting location, and the exit,
        //The complexity becomes even smaller!
        //This is a HUGE optimization for my Alpha-beta pruning, and perhaps I can even combine it
        //with some forms of Memoization to really speed up my code!
        //I think with all these factors, this problem might actually be possible!

        //I will now create a 2d map called allPaths!
        //Alright, my algo still struggles, even though sometimes it really can solve the problem!
        //One way I can actually optimize this further is instead of using a map 2d map, I use a
        //sorted array INSIDE a map!
        //The array would be sorted by using a custom comparator with Arrays.sort where I would sort
        //by proximity of other to node (aka bestPath.size())
        //I can use an array instead of a list because I know all the sizes of the array:
        //coinIDs.size() + 1!
        //With this, I add an extra layer of greedy to the pruning algorithm to hopefully find the
        //path that gets the max coins even faster (and so it can still survive with large numbers
        //of coins)
        //Alright, lets make this cool!
        Map<Long,Entry<Long,Entry<Long,List<Edge>>>[]> allPaths = new HashMap<>();
        for(Node node: state.allNodes()){
            if(coinIds.contains(node.getId()) || node.getId() == state.currentNode().getId() ||
                    node.getId() == state.exit().getId()){
                Entry<Long,Entry<Long,List<Edge>>>[] nodePaths = new Entry[coinIds.size()+1];

                dijkstra.singleSourceDistances(node);
                int i = 0;
                for(Node other: state.allNodes()){
                    if((coinIds.contains(other.getId()) ||other.getId()==state.currentNode().getId()
                            || other.getId()==state.exit().getId()) && node.getId()!=other.getId()){
                        List<Edge> nodePath = dijkstra.bestPath(other);
                        long weight = 0;
                        for(Edge edge: nodePath){
                            weight+=edge.length();
                        }
                        nodePaths[i++]= new SimpleEntry<>(other.getId(),
                                new SimpleEntry<>(weight,nodePath));
                    }
                }
                //Now simply sort nodePaths using a custom comparator and put it into the map!

                Arrays.sort(nodePaths, Comparator.comparingDouble(a ->
                        a.getValue().getKey()/(map.get(a.getKey()).getTile().coins() * 1d)));
                allPaths.put(node.getId(),nodePaths);

            }


        }
        /*System.out.println(map);
        //System.out.println(allPaths);
        System.out.println(state.currentNode().getId());
        System.out.println(paths);
        System.out.println(coins);
        System.out.println(paths.get(state.exit().getId()));
        System.out.println(allPaths.size() + "/"+ allPaths.get(state.currentNode().getId()).length);
        System.out.println(state.stepsToGo());*/

        long[] statics = new long[]{coins,0,System.currentTimeMillis()};
        //Alright, time to start the alpha-beta pruning!
        Entry<Integer,List<Edge>> ans = prune(allPaths,state.currentNode().getId(),map,
                new ArrayList<>(), 0b0,state.currentNode().getTile().originalCoinValue(),
                statics,state,exitDist);
        /*System.out.println(ans);
        System.out.println(ans.getValue().size());
        System.out.println(num);*/

        //OMG IT WORKED!!! IT WORKED!!!
        //IT WAS SO FAST! I CAN'T BELIEVE IT!
        //Alright, time to make the moves :D
        //NEVERMIND SCREW THIS PROGRAM, SCREW THE NEED FOR UNNECESSARY EDGE WEIGHTS!!!!
        //Ya... unfortunately most of my work was for naught... at least the end result is ok...
        Node node = state.currentNode();
        for(Edge edge: ans.getValue()){
            state.moveTo(edge.getOther(node));
            node = edge.getOther(node);
        }



    }


    //Prunes through allPaths to find an optimal path that gets all maxCoins in numMoves time!
    //now optimized with greedy sorting of arrays within allPaths to hopefully find the best path
    //even faster!!!
    private Entry<Integer,List<Edge>> prune(Map<Long,Entry<Long,Entry<Long,List<Edge>>>[]> allPaths,
            long id, Map<Long,Node> map, List<Edge> path, long pathWeight, int val, long[] statics,
            ScramState state, Map<Long,Long> exitDist){

        if(pathWeight +exitDist.get(id) > state.stepsToGo() || allPaths.isEmpty() ||
                allPaths.get(id) == null ){
            return new SimpleEntry<>(-1,null);
        }

        if(id == state.exit().getId()){
            statics[1] = Math.max(val,statics[1]);
            return new SimpleEntry<>(val,path);
        }

        Entry<Long,Entry<Long,List<Edge>>>[] paths = allPaths.remove(id);
        int coins;
        long newPathWeight;
        List<Edge> newPath;
        int bestCoins = -1;
        Entry<Integer,List<Edge>> bestAns = null;
        for(Entry<Long,Entry<Long,List<Edge>>> node: paths){
            if(!allPaths.containsKey(node.getKey())){
                continue;
            }
            coins= val + map.get(node.getKey()).getTile().coins();
            newPath = new ArrayList<>(path);
            newPath.addAll(node.getValue().getValue());
            //Stupid traversal in order to account for stupid edge weights...this is so stupid! >:(
            //Yay, I fixed it so now I no longer have to do a stupid for loop :/
            newPathWeight = pathWeight +node.getValue().getKey();
            if(coins < statics[1] && newPathWeight + exitDist.get(node.getKey()) >=
                    state.stepsToGo() + allPaths.get(node.getKey())[0].getValue().getKey()){
                continue;
            }
            Entry<Integer,List<Edge>> ans = prune(allPaths, node.getKey(), map, newPath,
                    newPathWeight, coins, statics, state,exitDist);
            if(ans.getKey() >= bestCoins){
                bestCoins = ans.getKey();
                bestAns = ans;
            }
            //Check if the value of ans is the best possible answer or if 9 seconds has elapsed
            //Most often the recursion will end by the 2nd criteria, which allows for the pruning
            //to finish even though the best answer (or even a decent answer) has not been found :(
            if(ans.getKey() == statics[0] || statics[2]+ 9900 < System.currentTimeMillis()){
                return bestAns;
            }

        }
        allPaths.put(id,paths);
        return bestAns;

    }

    //ALERT! ALL CODE BELOW THIS POINT IS DEPRECATED
    //read at your own risk!


    //Recurses as a DFS through the node map in order to populate the dp!
    //Unfortunately, due to the existence of the set, this forces infinite recursion as there are
    //way too many combinations.
    //As such, I'll have to employ alpha-beta pruning and/or other forms of pruning in able to
    //reduce the number of total calls to something reasonable
    //One easy to implement optimization is simply adding a Dijkstra's, using ShortestPaths to
    //represent the number of moves, and best route in order to reach the exit
    //However, since the method calculates the best way to go from a source vertex to every other
    //vertex, but I want the best path from any vertex to a specific exit vertex, I can just call
    //singleSourceDistance with the exit as the source vertex and my current vertex as the target
    //vertex and simply just travel the path backwards!
    //What I can do is precompute this for every node, and simply check a hashmap to see if I can
    //make it to the exit in the remaining number of moves, and when I have exactly that number of
    //moves left, head for the exit.
    //This won't help during the early layers of iteration, but as the number of steps gets smaller
    //it'll prove a very helpful optimization!
    //Furthermore, one huge optimization that can help greatly reduce complexity is completely
    //stopping recursion as soon as the max number of coins is achieved
    //However, this is slightly harder said than done as completely breaking out of recursion is
    //quite tricky.
    //One way I could try and do this is by making the method actually return something, which is
    //a boolean representing if the max coin value is obtained at dp[dp.length-1][state.exit()]!
    //If I ever get return the maxVal, make sure to escape as soon as possible!
    //Next optimization I am going to try would be to create a super large 2d map of n^2 which
    //represents the optimal path from any node to any other node
    //I can also change the structure of the coin set to contain all the ids of nodes that still
    //have coins
    //With this information, I can then apply some real alpha-beta pruning by looking at each
    //location that still has coins left and simply calculating, using the... wait, no I have a new
    //idea, give me a sec!
    /*private int recurse(int val, Map<Long,Node> map, long id, int i, int maxCoins, List<Edge> path,
            long exit, Map<Long,List<Edge>> paths, Set<Long> coins){
        //Base case for when McDiver reaches the exit:
        //If he reaches the exit with 1 move left or with a bunch of moves left, it doesn't matter,
        //the program ends and he leaves.
        //As such, if id ever equals exit, then I simply 'pass' the move and copy over dp[i][j] to
        //dp[i+1][j] if dp[i][j] > dp[i+1][j]

        if(i == 0 || id == exit){
            num++;
            if(val > maxVal){
                maxVal = val;
            }
            if(num%10000 == 0){
                System.out.println(num +"/" + val +"/" + maxVal);
            }
            //If the max value is obtained, return
            if(val == maxCoins){
                return val;
            }
            return -1 * val;
        }
        //First check if it is even possible with the remaining moves to make it the exit
        //This itself should work well enough in order to force McDiver to exit, but I might want to
        //force even stronger by forcing McDiver to follow the path... I will try that after seeing
        //what happens with just this!
        if(paths.get(id).size() > i){
            num++;
            return -1;
        }

        //Alright, I will now try forcing stronger by recognizing if you only have one or two moves
        //left, find your way to the exit
        //I will then eventually change it so that selection for when you must exit is more accurate
        //but for now this should be good enough
        if(paths.get(id).size() >= i+1){
            List<Edge> pathToExit = paths.get(id);
            Node node = pathToExit.get(pathToExit.size()-1).getOther(map.get(id));
            return processNode(val,node,map,id,i,maxCoins,path,exit,paths,coins);
        }else {
            int maxVal = 0;
            List<Edge> newPath;
            for (Node node : map.get(id).getNeighbors()) {
                newPath = new ArrayList<>(path);
                int nodeVal = processNode(val,node,map,id,i,maxCoins,newPath,exit,paths,coins);
                if(nodeVal > 0){
                    System.out.println(nodeVal);
                    return nodeVal;
                }
                if(nodeVal < maxVal){
                    maxVal = nodeVal;
                }
            }
            return maxVal;
        }
    }

    //Helper method for the main step of process each node!
    //I just realized... dp doesn't work at all here :'(
    //I have to completely rewrite the entire method...
    private int processNode(int val, Node node, Map<Long,Node> map, long id, int i, int maxCoins,
            List<Edge> path, long exit, Map<Long,List<Edge>> paths, Set<Long> coins){

        long nodeId = node.getId();
        int coinBonus = !coins.contains(nodeId) ? map.get(nodeId).getTile().coins() : 0;
        int nodeVal = val+ coinBonus;
        path.add(map.get(id).getEdge(node));
        if (!coins.contains(nodeId)) {
            coins.add((long) nodeId);
            int maxVal = recurse(nodeVal, map, nodeId,i - 1,maxCoins, path ,exit, paths, coins);
            coins.remove((long) nodeId);
            return maxVal;
        } else {
            return recurse(nodeVal, map, nodeId, i - 1, maxCoins, path,exit, paths, coins);
        }

        //uh oh... I completely forgot about the case where you make some unoptimal moves in order
        //to find the correct answer :'(, I'll have to completely rewrite much of my code
        //I'm going to ignore this for now... but I'll definitely have to redo this

    }

/*private boolean processNode(int[][][] dp, Node node, Map<Long,Node> map, long id, int i,
            int maxCoins, long exit, Map<Long,List<Edge>> paths, Set<Long> coins){

        long nodeId = node.getId();
        int coinBonus = !coins.contains(nodeId) ? map.get(nodeId).getTile().coins() : 0;
        if (dp[i + 1][(int) nodeId][0] <= dp[i][(int) id][0] + coinBonus) {
            dp[i + 1][(int) nodeId][0] = dp[i][(int) id][0];
            dp[i + 1][(int) nodeId][1] = (int) id;
            if (!coins.contains(nodeId)) {
                dp[i + 1][(int) nodeId][0] += map.get(nodeId).getTile().coins();
                coins.add((long) nodeId);
                boolean goal = recurse(dp, map, nodeId, i + 1, maxCoins, exit, paths, coins);
                coins.remove((long) nodeId);
                return goal;
            } else {
                return recurse(dp, map, nodeId, i + 1, maxCoins, exit, paths, coins);
            }
        }
        //uh oh... I completely forgot about the case where you make some unoptimal moves in order
        //to find the correct answer :'(, I'll have to completely rewrite much of my code
        //I'm going to ignore this for now... but I'll definitely have to redo this
        return false;

      }
 */



     /* ALL CODE BELOW THIS POINT IS ALSO DEPRECATED

        This code was moved from my DoScram method as it was used in my old pruning attempt which
        was really, really bad!



        //My initial dp construction, using max Node ID for j bounds and numMoves for i bounds
        int[][][] dp = new int[state.stepsToGo()][(int)(maxID)+1][2];
        //Now create a DFS to start my traversal and calculate max coin values!
        //I use a dfs instead of a bfs because I need to mark my coins as taken
        //This dfs will be done through a recursive function called recurse
        //Technically, this might be memoization, but it feels a lot more like tabulation dp as
        //I don't 'memorize' results like in memoization!
        /*Deque<Long> dfs = new ArrayDeque<>();
        bfs.offer(state.currentNode().getId());
        for(int i = 0; i < state.stepsToGo()-1 && !bfs.isEmpty(); i++){
            long id = bfs.poll();
            for(Node node:map.get(id).getNeighbors()){
                int nodeId = (int)node.getId();
                if(dp[i+1][nodeId][0] <= dp[i][(int)id][0]){
                    dp[i+1][nodeId][0] = dp[i][(int)id][0]+node.tile.takeCoins();

                }
            }
        }

        List<Edge> path = new ArrayList<>();
        int ans = recurse(0,map,(int)state.currentNode().getId(),state.stepsToGo(), coins, path,
                state.exit().getId(), paths, new HashSet<>());
        //System.out.println(Arrays.deepToString(dp));
        //System.out.println(Arrays.toString(dp[dp.length-1][(int)state.exit().getId()]));
        System.out.println(ans);
        System.out.println(path);
        */


}



