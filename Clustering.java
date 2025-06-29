import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.awt.Color;

public class Clustering {
	private EdgeWeightedGraph G;
	private List<List<Integer>> clusters;
	private List<List<Integer>> labeled;
	private boolean verboseMode = false;

	public Clustering(EdgeWeightedGraph graph) {
		this.G = graph;
		this.clusters = new LinkedList<List<Integer>>();
	}

	public Clustering(In in) {
		int V = in.readInt();
		int dim = in.readInt();
		this.G = new EdgeWeightedGraph(V);
		this.labeled = new LinkedList<List<Integer>>();

		List<String> seenLabels = new ArrayList<String>();
		double[][] coords = new double[V][dim];

		for (int i = 0; i < V; i++) {
			for (int d = 0; d < dim; d++) {
				coords[i][d] = in.readDouble();
			}

			String label = in.readString();
			int labelIndex = seenLabels.indexOf(label);

			if (labelIndex != -1) {
				labeled.get(labelIndex).add(i);
			} else {
				seenLabels.add(label);
				List<Integer> newGroup = new LinkedList<Integer>();
				newGroup.add(i);
				labeled.add(newGroup);
			}
		}

		G.setCoordinates(coords);

		for (int a = 0; a < V; a++) {
			for (int b = a+1; b < V; b++) {
				double sum = 0.0;
				for (int k = 0; k < dim; k++) {
					double diff = coords[a][k] - coords[b][k];
					sum += diff * diff;
				}
				double weight = Math.sqrt(sum);
				G.addEdge(new Edge(a, b, weight));
				G.addEdge(new Edge(b, a, weight));
			}
		}
		this.clusters = new LinkedList<List<Integer>>();
	}

	public void findClusters(int k) {
		PrimMST mst = new PrimMST(G);
		// Fix: Convert Iterable<Edge> to ArrayList<Edge> properly
		List<Edge> edges = new ArrayList<Edge>();
		for (Edge e : mst.edges()) {
			edges.add(e);
		}

		Collections.sort(edges, Collections.reverseOrder());

		List<Edge> edgesToKeep = edges.subList(k-1, edges.size());

		UF uf = new UF(G.V());
		for (Edge e : edgesToKeep) {
			uf.union(e.either(), e.other(e.either()));
		}

		collectClusters(uf);
		if (verboseMode) {
			System.out.println("Made " + clusters.size() + " clusters");
		}
	}

	public void findClusters(double threshold) {
		PrimMST mst = new PrimMST(G);
		// Fix: Convert Iterable<Edge> to ArrayList<Edge> properly
		List<Edge> edges = new ArrayList<Edge>();
		for (Edge e : mst.edges()) {
			edges.add(e);
		}

		Collections.sort(edges, Collections.reverseOrder());

		List<Edge> currentEdges = new ArrayList<Edge>(edges);
		while (!currentEdges.isEmpty() && coefficientOfVariation(currentEdges) > threshold) {
			currentEdges.remove(0);
		}

		UF uf = new UF(G.V());
		for (Edge e : currentEdges) {
			uf.union(e.either(), e.other(e.either()));
		}
		collectClusters(uf);
	}

	private void collectClusters(UF uf) {
		clusters.clear();
		Map<Integer, Integer> clusterMap = new HashMap<Integer, Integer>();
		int nextId = 0;

		for (int v = 0; v < G.V(); v++) {
			int root = uf.find(v);
			if (!clusterMap.containsKey(root)) {
				clusterMap.put(root, nextId++);
				clusters.add(new LinkedList<Integer>());
			}
			clusters.get(clusterMap.get(root)).add(v);
		}
	}

	public int[] validation() {
		if (labeled == null || labeled.isEmpty()) {
			return new int[0];
		}

		int[] results = new int[clusters.size()];

		for (int i = 0; i < clusters.size(); i++) {
			int[] counts = new int[labeled.size()];

			for (int node : clusters.get(i)) {
				for (int j = 0; j < labeled.size(); j++) {
					if (labeled.get(j).contains(node)) {
						counts[j]++;
					}
				}
			}

			int max = 0;
			for (int count : counts) {
				if (count > max) max = count;
			}
			results[i] = max;
		}
		return results;
	}

	public double coefficientOfVariation(List<Edge> edges) {
		if (edges == null || edges.isEmpty()) {
			return 0.0;
		}

		double sum = 0.0;
		for (Edge e : edges) {
			sum += e.weight();
		}
		double mean = sum / edges.size();

		double variance = 0.0;
		for (Edge e : edges) {
			double diff = e.weight() - mean;
			variance += diff * diff;
		}
		variance /= edges.size();

		return Math.sqrt(variance) / mean;
	}

	public void plotClusters() {
		int size = 800;
		StdDraw.setCanvasSize(size, size);
		StdDraw.setXscale(0, 15);
		StdDraw.setYscale(0, 15);
		StdDraw.clear(Color.BLACK);

		Color[] colors = {
				Color.WHITE,
				new Color(128, 0, 0),
				Color.GRAY,
				new Color(0, 108, 173),
				new Color(45, 139, 48),
				new Color(226, 126, 38),
				new Color(132, 67, 172)
		};

		int colorIndex = 0;
		for (List<Integer> cluster : clusters) {
			StdDraw.setPenColor(colors[colorIndex % colors.length]);
			StdDraw.setPenRadius(0.02);

			for (int v : cluster) {
				double[] coords = G.getCoordinates()[v];
				StdDraw.point(coords[0], coords[1]);
			}

			colorIndex++;
		}
		StdDraw.show();
	}

	public static void main(String[] args) {
		if (args.length < 2) {
			System.err.println("Usage: java Clustering <filename> <k>");
			return;
		}

		In in = new In(args[0]);
		int k = Integer.parseInt(args[1]);

		Clustering clusterer = new Clustering(in);
		clusterer.findClusters(k);

		int[] results = clusterer.validation();
		System.out.println("Results: " + Arrays.toString(results));
	}
}
