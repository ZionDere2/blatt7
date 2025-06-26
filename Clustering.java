import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.awt.Color;

/**
 * This class solves a clustering problem with the Prim algorithm.
 */
public class Clustering {
	EdgeWeightedGraph G;
	List <List<Integer>>clusters; 
	List <List<Integer>>labeled; 
	
	/**
	 * Constructor for the Clustering class, for a given EdgeWeightedGraph and no labels.
	 * @param G a given graph representing a clustering problem
	 */
	public Clustering(EdgeWeightedGraph G) {
            this.G=G;
	    clusters= new LinkedList <List<Integer>>();
	}
	
    /**
	 * Constructor for the Clustering class, for a given data set with labels
	 * @param in input file for a clustering data set with labels
	 */
	public Clustering(In in) {
            int V = in.readInt();
            int dim= in.readInt();
            G= new EdgeWeightedGraph(V);
            labeled=new LinkedList<List<Integer>>();
            List<String> labels = new LinkedList<String>();
            double[][] coord = new double [V][dim];
            for (int v = 0;v<V; v++ ) {
                for(int j=0; j<dim; j++) {
                	coord[v][j]=in.readDouble();
                }
                String label= in.readString();
                int idx = labels.indexOf(label);
                if(idx != -1) {
                        labeled.get(idx).add(v);
                } else {
                        labels.add(label);
                        List<Integer> l= new LinkedList<Integer>();
                        l.add(v);
                        labeled.add(l);
                }
            }
             
            G.setCoordinates(coord);
            for (int w = 0; w < V; w++) {
                for (int v = 0;v<V; v++ ) {
                	if(v!=w) {
                	double weight=0;
                    for(int j=0; j<dim; j++) {
                    	weight= weight+Math.pow(G.getCoordinates()[v][j]-G.getCoordinates()[w][j],2);
                    }
                    weight=Math.sqrt(weight);
                    Edge e = new Edge(v, w, weight);
                    G.addEdge(e);
                	}
                }
            }
	    clusters= new LinkedList <List<Integer>>();
	}
	
    /**
	 * This method finds a specified number of clusters based on a MST.
	 *
	 * It is based in the idea that removing edges from a MST will create a
	 * partition into several connected components, which are the clusters.
	 * @param numberOfClusters number of expected clusters
	 */
        public void findClusters(int numberOfClusters){
            PrimMST mst = new PrimMST(G);
            List<Edge> edges = new LinkedList<Edge>();
            for (Edge e : mst.edges()) {
                edges.add(e);
            }
            Collections.sort(edges); // ascending by weight

            UF uf = new UF(G.V());
            int edgesToKeep = edges.size() - (numberOfClusters - 1);
            for (int i = 0; i < edgesToKeep; i++) {
                Edge e = edges.get(i);
                int v = e.either();
                int w = e.other(v);
                uf.union(v, w);
            }
            clusters = connectedComponents(uf);
        }
	
	/**
	 * This method finds clusters based on a MST and a threshold for the coefficient of variation.
	 *
	 * It is based in the idea that removing edges from a MST will create a
	 * partition into several connected components, which are the clusters.
	 * The edges are removed based on the threshold given. For further explanation see the exercise sheet.
	 *
	 * @param threshold for the coefficient of variation
	 */
        public void findClusters(double threshold){
            PrimMST mst = new PrimMST(G);
            List<Edge> sorted = new LinkedList<Edge>();
            for (Edge e : mst.edges()) {
                sorted.add(e);
            }
            Collections.sort(sorted);

            List<Edge> solution = new LinkedList<Edge>();
            for (Edge e : sorted) {
                solution.add(e);
                if (coefficientOfVariation(solution) > threshold) {
                    solution.remove(solution.size() - 1);
                }
            }

            UF uf = new UF(G.V());
            for (Edge e : solution) {
                int v = e.either();
                int w = e.other(v);
                uf.union(v, w);
            }
            clusters = connectedComponents(uf);
        }
	
	/**
	 * Evaluates the clustering based on a fixed number of clusters.
	 * @return array of the number of the correctly classified data points per cluster
	 */
        public int[] validation() {
            if (labeled == null || clusters == null) return new int[0];
            // sort clusters and labeled lists for consistent order
            Collections.sort(clusters, (a,b) -> Integer.compare(a.get(0), b.get(0)));
            Collections.sort(labeled, (a,b) -> Integer.compare(a.get(0), b.get(0)));

            int n = Math.min(clusters.size(), labeled.size());
            int[] result = new int[n];
            for (int i = 0; i < n; i++) {
                List<Integer> cl = clusters.get(i);
                List<Integer> lab = labeled.get(i);
                int count = 0;
                for (int v : cl) {
                    if (lab.contains(v)) count++;
                }
                result[i] = count;
            }
            return result;
        }
	
	/**
	 * Calculates the coefficient of variation.
	 * For the formula see exercise sheet.
	 * @param part list of edges
	 * @return coefficient of variation
	 */
        public double coefficientOfVariation(List <Edge> part) {
            if (part == null || part.size() <= 1) return 0.0;
            double sum = 0.0;
            double sq = 0.0;
            for (Edge e : part) {
                double w = e.weight();
                sum += w;
                sq += w * w;
            }
            double mean = sum / part.size();
            double variance = sq / part.size() - mean * mean;
            if (variance < 0) variance = 0; // numerical safety
            double sd = Math.sqrt(variance);
            return sd / mean;
        }

    // helper method to compute connected components using UF structure
    private List<List<Integer>> connectedComponents(UF uf) {
        java.util.Map<Integer, List<Integer>> map = new java.util.HashMap<>();
        for (int v = 0; v < G.V(); v++) {
            int root = uf.find(v);
            List<Integer> l = map.get(root);
            if (l == null) {
                l = new LinkedList<Integer>();
                map.put(root, l);
            }
            l.add(v);
        }
        List<List<Integer>> comps = new LinkedList<List<Integer>>();
        for (List<Integer> l : map.values()) {
            Collections.sort(l);
            comps.add(l);
        }
        Collections.sort(comps, (a,b) -> Integer.compare(a.get(0), b.get(0)));
        return comps;
    }
	
	/**
	 * Plots clusters in a two-dimensional space.
	 */
	public void plotClusters() {
		int canvas=800;
	    StdDraw.setCanvasSize(canvas, canvas);
	    StdDraw.setXscale(0, 15);
	    StdDraw.setYscale(0, 15);
	    StdDraw.clear(new Color(0,0,0));
		Color[] colors= {new Color(255, 255, 255), new Color(128, 0, 0), new Color(128, 128, 128), 
				new Color(0, 108, 173), new Color(45, 139, 48), new Color(226, 126, 38), new Color(132, 67, 172)};
	    int color=0;
		for(List <Integer> cluster: clusters) {
			if(color>colors.length-1) color=0;
		    StdDraw.setPenColor(colors[color]);
		    StdDraw.setPenRadius(0.02);
		    for(int i: cluster) {
		    	StdDraw.point(G.getCoordinates()[i][0], G.getCoordinates()[i][1]);
		    }
		    color++;
	    }
	    StdDraw.show();
	}
	

    public static void main(String[] args) {
		// FOR TESTING
    }
}

