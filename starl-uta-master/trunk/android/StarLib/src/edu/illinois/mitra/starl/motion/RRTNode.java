package edu.illinois.mitra.starl.motion;

import java.awt.Point;
import java.util.Stack;

import edu.illinois.mitra.starl.objects.ItemPosition;
import edu.illinois.mitra.starl.objects.ObstacleList;
import edu.wlu.cs.levy.CG.KDTree;

/**
 * This implements RRT path finding algorithm using kd tree
 * 
 * @author Yixiao Lin
 * @version 1.0
 */

public class RRTNode {
	public Point position = new Point();
	public RRTNode parent;
	public static RRTNode stopNode;
	public KDTree<RRTNode> kd;
//	public LinkedList<ItemPosition> pathList = new LinkedList<ItemPosition>();

	public double [] getValue(){
		double [] toReturn = {position.x,position.y}; 
		return toReturn;
	}

	public RRTNode(){
		position.x = 0;
		position.y = 0;
		parent = null;
	}
	
	public RRTNode(int x, int y){
		position.x = x;
		position.y = y;
		parent = null;
	}

	public RRTNode(RRTNode copy){
		position.x = copy.position.x;
		position.y = copy.position.y;
		parent = copy.parent;
	}

//methods to find the route
//if find a path, return a midway point stack
//if can not find a path, return null
//remember to handle the null stack when writing apps using RRT path planning
//the obstacle list will be modified to remove any obstacle that is inside a robot
	
    public Stack<ItemPosition> findRoute(ItemPosition destination, int K, ObstacleList obsList, int xRange, int yRange, ItemPosition RobotPos, int Radius) {
//initialize a kd tree;
    	obsList.remove(RobotPos, 0.9*RobotPos.radius);
    	kd = new KDTree<RRTNode>(2);
    	double [] root = {position.x,position.y};
    	final RRTNode rootNode = new RRTNode(position.x,position.y);
    	final RRTNode destNode = new RRTNode(destination.x, destination.y);
    	
    	try{
    		kd.insert(root, rootNode);
    	}
    	catch(Exception e){
    		System.err.println(e);
    	}
    	
    	
    	RRTNode currentNode = new RRTNode(rootNode);
    	RRTNode addedNode = new RRTNode(rootNode);
    //for(i< k)  keep finding	
    	for(int i = 0; i<K; i++){
    	//if can go from current to destination, meaning path found, add destinationNode to final, stop looping.
    		if(obsList.validPath(addedNode, destNode, 165)){
    			
    			destNode.parent = addedNode;
    			stopNode = destNode;
    			try{	
    			kd.insert(destNode.getValue(), destNode);
    			}
        		catch (Exception e) {
        		    System.err.println(e);
        		}
    			//System.out.println("Path found!");
    			break;
    		}
    		//not find yet, keep exploring
    		//random a sample point in the valid set of space
    		boolean validRandom = false;
    		int xRandom = 0;
    		int yRandom = 0;
    		ItemPosition sampledPos = new ItemPosition("rand",xRandom, yRandom, 0);
    		while(!validRandom){
    			xRandom = (int)(Math.random() * ((xRange) + 1));
        		yRandom = (int)(Math.random() * ((yRange) + 1));
        		sampledPos.x = xRandom;
        		sampledPos.y = yRandom;
        		validRandom = obsList.validstarts(sampledPos, Radius); 	
    		}
    		RRTNode sampledNode = new RRTNode(sampledPos.x, sampledPos.y);
    		// with a valid random sampled point, we find it's nearest neighbor in the tree, set it as current Node
    		try{
    		currentNode = kd.nearest(sampledNode.getValue());
    		}
    		catch (Exception e) {
    		    System.err.println(e);
    		}
    		sampledNode = toggle(currentNode, sampledNode, obsList, Radius);
    		//check if toggle failed
    		//if not failed, insert the new node to the tree
    		if(sampledNode != null){
    			sampledNode.parent = currentNode;
    			try{
    	    		kd.insert(sampledNode.getValue(), sampledNode);
    	    		}
    	    		catch (Exception e) {
    	    		    System.err.println(e);
    	    		}
    			//set currentNode as newest node added, so we can check if we can reach the destination
    			addedNode = sampledNode;
    			//
    		}
    	}
    	stopNode = addedNode;
	
    	//after searching, we update the path to a stack

      	RRTNode curNode = destNode;  	
		Stack<ItemPosition> pathStack= new Stack<ItemPosition>();
		while(curNode != null){
			ItemPosition ToGo= new ItemPosition("midpoint", curNode.position.x, curNode.position.y, 0);
			pathStack.push(ToGo);
			curNode = curNode.parent;
		}
    	
    	if(destNode.parent == null){
    		System.out.println("Path Not found!");
    		return(null);
    	}
    	else{
    		stopNode = destNode;
    		return pathStack;
    
    	}
    }

	private RRTNode toggle(RRTNode currentNode, RRTNode sampledNode, ObstacleList obsList, int radius) {
		// toggle function deals with constrains by the environment as well as robot systems. 
		// It changes sampledNode to some point alone the line of sampledNode and currentNode so that no obstacles are in the middle
		// In other words, it changes sampledNode to somewhere alone the line where robot can reach
		
		// we can add robot system constraints later
		
		RRTNode toggleNode = new RRTNode(sampledNode);
		int tries = 0;
		// try 20 times, which will shorten it to 0.00317 times the original path length
		// smaller tries might make integer casting loop forever
		while((!obsList.validPath(toggleNode, currentNode, radius)) && (tries < 20))
		{
			//move 1/4 toward current
			toggleNode.position.x = (int) ((toggleNode.position.x + currentNode.position.x)/(1.5));
			toggleNode.position.y = (int) ((toggleNode.position.y + currentNode.position.y)/(1.5));
			tries ++;
		}
		//return currentNode if toggle failed
		if(tries >= 19)
			return null;
		else
			return toggleNode;
	}

	
}

