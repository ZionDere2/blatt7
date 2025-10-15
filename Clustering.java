import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.awt.Color;

/**
 * Utility class that groups vertices of an {@link EdgeWeightedGraph}
 * into clusters by cutting edges of its minimum spanning tree.
 */
public class Clustering {
        private EdgeWeightedGraph graph;
        private List<List<Integer>> clusters;
        private List<List<Integer>> labeled;
        private boolean verbose = false;

        /**
         * Creates a clusterer from an existing graph.
         */
        public Clustering(EdgeWeightedGraph graph) {
                this.graph = graph;
                this.clusters = new LinkedList<>();
        }

	public Clustering(In in) {
		int V = in.readInt();
		int dim = in.readInt();
                this.graph = new EdgeWeightedGraph(V);
                this.labeled = new LinkedList<>();

                List<String> uniqueLabels = new ArrayList<>();
                double[][] coords = new double[V][dim];

		for (int i = 0; i < V; i++) {
			for (int d = 0; d < dim; d++) {
				coords[i][d] = in.readDouble();
			}

                        String label = in.readString();
                        int labelIndex = uniqueLabels.indexOf(label);

                        if (labelIndex != -1) {
                                labeled.get(labelIndex).add(i);
                        } else {
                                uniqueLabels.add(label);
                                List<Integer> group = new LinkedList<>();
                                group.add(i);
                                labeled.add(group);
                        }
                }

                graph.setCoordinates(coords);

                for (int a = 0; a < V; a++) {
                        for (int b = a + 1; b < V; b++) {
                                double sum = 0.0;
                                for (int k = 0; k < dim; k++) {
                                        double diff = coords[a][k] - coords[b][k];
                                        sum += diff * diff;
                                }
                                double weight = Math.sqrt(sum);
                                graph.addEdge(new Edge(a, b, weight));
                                graph.addEdge(new Edge(b, a, weight));
                        }
                }
                this.clusters = new LinkedList<>();
        }

        /**
         * Forms {@code k} clusters by removing the {@code k-1} most
         * expensive edges from the minimum spanning tree of the graph.
         */
        public void findClusters(int k) {
                PrimMST mst = new PrimMST(graph);
                // collect MST edges in a mutable list
                List<Edge> mstEdges = new ArrayList<>();
                for (Edge e : mst.edges()) {
                        mstEdges.add(e);
                }

                Collections.sort(mstEdges, Collections.reverseOrder());

                for (int i = 0; i < k - 1 && !mstEdges.isEmpty(); i++) {
                        mstEdges.remove(0);
                }

                UF uf = new UF(graph.V());
                for (Edge e : mstEdges) {
                        uf.union(e.either(), e.other(e.either()));
                }

		collectClusters(uf);
                if (verbose) {
                        System.out.println("Made " + clusters.size() + " clusters");
                }
        }

        /**
         * Forms clusters by repeatedly removing the heaviest edge from the
         * minimum spanning tree until the coefficient of variation falls
         * below the provided threshold.
         */
        public void findClusters(double threshold) {
                PrimMST mst = new PrimMST(graph);
                // start with all edges of the MST
                List<Edge> remaining = new ArrayList<>();
                for (Edge e : mst.edges()) {
                        remaining.add(e);
                }

                Collections.sort(remaining, Collections.reverseOrder());

                while (!remaining.isEmpty() && coefficientOfVariation(remaining) > threshold) {
                        remaining.remove(0);
                }

                UF uf = new UF(graph.V());
                for (Edge e : remaining) {
                        uf.union(e.either(), e.other(e.either()));
                }
                collectClusters(uf);
        }

	private void collectClusters(UF uf) {
		clusters.clear();
		Map<Integer, Integer> clusterMap = new HashMap<Integer, Integer>();
		int nextId = 0;

                for (int v = 0; v < graph.V(); v++) {
                        int root = uf.find(v);
                        if (!clusterMap.containsKey(root)) {
                                clusterMap.put(root, nextId++);
                                clusters.add(new LinkedList<>());
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

        /**
         * Computes the coefficient of variation of the given edge weights.
         * A higher value means the edge weights vary more strongly.
         */
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
                                double[] coords = graph.getCoordinates()[v];
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
