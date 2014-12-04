package io.gamemachine.pathfinding;

import io.gamemachine.util.Vector3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;
import com.badlogic.gdx.utils.Array;

public class GridGraph implements IndexedGraph<Node> {

	public static boolean useDiagonals = true;
	public static double maxSlope = 45.0;
	public static double maxSmoothSlope = 35d;
	public static double maxStep = 0.4;
	public static double baseConnectionCost = 1f;
	
	public static Double[] slopePenalties = new Double[] {35d,2d,25d,1d};
	public static Double[] heightPenalties = new Double[] {10d,1d};
		
	public static enum Heuristic {MANHATTAN, MANHATTAN2D, EUCLIDEAN};
	public static Heuristic heuristic = Heuristic.MANHATTAN2D;

	public ProximityGrid grid;

	public Node[][] nodes;
	public HashMap<Integer, Node> nodeIndex = new HashMap<Integer, Node>();
	public int height;
	public int width;
	

	public GridGraph(int width, int height) {
		this.width = width;
		this.height = height;
		nodes = new Node[width][height];
		grid = new ProximityGrid(2000, 10);
	}

	@Override
	public int getNodeCount() {
		return nodeIndex.values().size();
	}

	@Override
	public Array<Connection<Node>> getConnections(Node fromNode) {
		return nodeIndex.get(fromNode.index).getConnections();
	}

	public Node findClosestNode(double x, double y) {
		int xi = (int) Math.round(x);
		int yi = (int) Math.round(y);
		return nodes[xi][yi];
	}

	public double distance(double x, double y, double otherX, double otherY) {
		return Math.sqrt(Math.pow(x - otherX, 2) + Math.pow(y - otherY, 2));
	}

	@SuppressWarnings("unchecked")
	public PathResult findPath(double startX, double startY, double endX, double endY, boolean useFunnel, boolean useCover) {
		PathResult pathResult = new PathResult();
		pathResult.startNode = findClosestNode(startX, startY);
		pathResult.endNode = findClosestNode(endX, endY);

		if (pathResult.startNode == null) {
			pathResult.error = "Node not found";
			return pathResult;
		}
		
		if (pathResult.startNode.connections.size == 0) {
			pathResult.error = "Node has no connections";
			return pathResult;
		}

		if (pathResult.endNode == null) {
			pathResult.error = "Node not found";
			return pathResult;
		}
		
		if (pathResult.endNode.connections.size == 0) {
			pathResult.error = "Node has no connections";
			return pathResult;
		}

		GridHeuristic he = new GridHeuristic();
		GridPathfinder<Node> finder = new GridPathfinder<Node>(this, true);
		pathResult.metrics = finder.metrics;
		pathResult.resultPath = new DefaultGraphPath<Node>();
		pathResult.result = finder.searchNodePath(pathResult.startNode, pathResult.endNode, he, pathResult.resultPath);
		if (pathResult.result) {
			if (useFunnel) {
				pathResult.smoothPath = Funnel2d.smoothPath(pathResult.startNode, pathResult.endNode,
						pathResult.resultPath.nodes);
				pathResult.smoothPathCount = pathResult.smoothPath.size();
			}

			if (useCover) {
				if (useFunnel) {
					pathResult.smoothPath = smoothPath(pathResult.smoothPath);
				} else {
					List<Vector3> vnodes = new ArrayList<Vector3>();
					for (Object obj : pathResult.resultPath.nodes) {
						Node node = (Node) obj;
						vnodes.add(node.position);
					}
					pathResult.smoothPath = smoothPath(vnodes);
					pathResult.smoothPath = smoothPath2(pathResult.smoothPath, 10);
					pathResult.smoothPath = smoothPath(pathResult.smoothPath);
				}
				
				pathResult.smoothPathCount = pathResult.smoothPath.size();
			}
		}

		return pathResult;
	}

	private List<Vector3> smoothPath(List<Vector3> points) {
		List<Vector3> smooth = new ArrayList<Vector3>();

		Vector3 start = points.get(0);
		smooth.add(start);
		Vector3 previous = null;
		Vector3 current = null;

		for (Vector3 point : points) {
			current = point;
			if (start != current) {
				int walkable = SuperCover.cover(nodes, start.x, start.y, current.x, current.y, 1d, 1d);
				if (walkable != 0) {
					smooth.add(previous);
					start = previous;
				}
				previous = current;
			}
		}
		smooth.add(current);
		return smooth;
	}

	private List<Vector3> smoothPath2(List<Vector3> points, int look) {
		List<Vector3> smooth = new ArrayList<Vector3>();
		smooth.add(points.get(0));

		int pathcount = points.size();
		int idx = 0;
		int lastWalkable = 0;
		
		for (int a = 0; a < points.size(); a++) {
			Vector3 start = points.get(idx);
			for (int i = 1; i < look; i++) {
				int currentIdx = idx + i;
				if (currentIdx >= pathcount) {
					break;
				}
				Vector3 end = points.get(currentIdx);
				int walkable = SuperCover.cover(nodes, start.x, start.y, end.x, end.y, 1d, 1d);
				if (walkable == 0) {
					lastWalkable = currentIdx;
				} else {
					lastWalkable = idx + 1;
					//System.out.println("Not walkable "+walkable+ " : "+start.toString()+" --> "+end.toString());
				}
			}
			
			Vector3 point = points.get(lastWalkable);
			smooth.add(point);
			
			if (point == points.get(pathcount - 1)) {
				break;
			} else {
				idx = lastWalkable;
			}
		}
		return smooth;
	}

}
