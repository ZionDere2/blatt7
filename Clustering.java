import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
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
            labeled=new LinkedList <List<Integer>>();
            LinkedList labels= new LinkedList();
            double[][] coord = new double [V][dim];
            for (int v = 0;v<V; v++ ) {
                for(int j=0; j<dim; j++) {
                	coord[v][j]=in.readDouble();
                }
                String label= in.readString();
                    if(labels.contains(label)) {
                    	labeled.get(labels.indexOf(label)).add(v);
                    }
                    else {
                    	labels.add(label);
                    	List <Integer> l= new LinkedList <Integer>();
                    	labeled.add(l);
                        labeled.get(labels.indexOf(label)).add(v);
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
        Collections.sort(edges, Collections.reverseOrder());

        UF uf = new UF(G.V());
        for (int i = numberOfClusters - 1; i < edges.size(); i++) {
            Edge e = edges.get(i);
            uf.union(e.either(), e.other(e.either()));
        }

        clusters.clear();
        Map<Integer, Integer> map = new HashMap<>();
        int idx = 0;
        for (int v = 0; v < G.V(); v++) {
            int root = uf.find(v);
            Integer pos = map.get(root);
            if (pos == null) {
                pos = idx++;
                map.put(root, pos);
                clusters.add(new LinkedList<Integer>());
            }
            clusters.get(pos).add(v);
        }
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
        List<Edge> edges = new LinkedList<Edge>();
        for (Edge e : mst.edges()) {
            edges.add(e);
        }
        Collections.sort(edges, Collections.reverseOrder());

        List<Edge> working = new LinkedList<Edge>(edges);
        while (working.size() > 0 && coefficientOfVariation(working) > threshold) {
            working.remove(0); // remove largest
        }

        UF uf = new UF(G.V());
        for (Edge e : working) {
            uf.union(e.either(), e.other(e.either()));
        }

        clusters.clear();
        Map<Integer, Integer> map = new HashMap<>();
        int idx = 0;
        for (int v = 0; v < G.V(); v++) {
            int root = uf.find(v);
            Integer pos = map.get(root);
            if (pos == null) {
                pos = idx++;
                map.put(root, pos);
                clusters.add(new LinkedList<Integer>());
            }
            clusters.get(pos).add(v);
        }
        }
	
	/**
	 * Evaluates the clustering based on a fixed number of clusters.
	 * @return array of the number of the correctly classified data points per cluster
	 */
        public int[] validation() {
        if (labeled == null || labeled.isEmpty()) {
            return new int[0];
        }
        int[] result = new int[clusters.size()];
        for (int i = 0; i < clusters.size(); i++) {
            int[] counts = new int[labeled.size()];
            for (int v : clusters.get(i)) {
                for (int l = 0; l < labeled.size(); l++) {
                    if (labeled.get(l).contains(v)) {
                        counts[l]++;
                    }
                }
            }
            int max = 0;
            for (int c : counts) {
                if (c > max) max = c;
            }
            result[i] = max;
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
        if (part == null || part.isEmpty()) return 0.0;
        double sum = 0.0;
        for (Edge e : part) {
            sum += e.weight();
        }
        double mean = sum / part.size();
        double var = 0.0;
        for (Edge e : part) {
            double diff = e.weight() - mean;
            var += diff * diff;
        }
        var /= part.size();
        double sd = Math.sqrt(var);
        return sd / mean;
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
        if (args.length < 2) {
            System.err.println("Usage: java Clustering <inputfile> <number_of_clusters>");
            return;
        }

        In in = new In(args[0]);
        int k = Integer.parseInt(args[1]);
        Clustering c = new Clustering(in);
        c.findClusters(k);
        int[] eval = c.validation();
        System.out.println(Arrays.toString(eval));
    }
}

