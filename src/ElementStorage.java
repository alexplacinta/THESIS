public class RelationEntityDAOImpl {

	private FramedGraph<OrientGraph> graph;
	private OrientGraphFactory orientGraphFactory;
	private NamingService namingService;

	public RelationEntityDAOImpl(
			FramedGraph<OrientGraph> graph,
			OrientGraphFactory orientGraphFactory,
			NamingService namingService
			) {
		this.graph = graph;
		this.orientGraphFactory = orientGraphFactory;
		this.namingService = namingService;
	}

	public GraphContent getGraphOfSuspiciousCase(String fromRid, String toRid) {
		OrientGraph orientGraph = null;
		try {
			orientGraph = orientGraphFactory.getTx();

			String query = "select *, eval(\"@class instanceof 'V'\") as isVertex from (select expand($a)" +
					" let $a = intersect(" +
					"    (select  from (traverse outE('contractInstitution','companyContract','companyFounder','personFounder','administrator'), inV() from #" + fromRid + " while true))," +
					"    (select  from (traverse inE('contractInstitution','companyContract','companyFounder','personFounder','administrator'), outV() from #" + toRid + " while true))" +
					"  ) )";

			OCommandSQL commandSQL = new OCommandSQL(query);
			Iterable<Element> elementsIt = orientGraph.command(commandSQL).execute();
			List<Element> elements = Lists.newLinkedList(elementsIt);

			List<Map<String, Object>> vertices = elements
					.stream()
					.filter(element -> (boolean)element.getProperty("isVertex"))
					.map(v -> {
						Map<String, Object> map = new HashMap<>();

						map.put("id", v.getId().toString());
						map.put("type", v.getProperty("@class"));

						if (v.getProperty("@class").equals("Contract")) {
							map.put("sum", v.getProperty("sum"));
							map.put("name", v.getProperty("objectDescription"));
						} else {
							map.put("name", namingService.getMoreClearEntityName((Vertex)v));
						}
						return map;
					})
					.collect(Collectors.toCollection(LinkedList::new));

			List<Map<String, Object>> edges = elements
					.stream()
					.filter(element -> !(boolean)element.getProperty("isVertex"))
					.map(e -> {
						Map<String, Object> map = new HashMap<>();

						map.put("id", e.getId().toString());
						map.put("from", ((OrientVertex) e.getProperty("out")).getId().toString());
						map.put("to", ((OrientVertex) e.getProperty("in")).getId().toString());
						map.put("type", e.getProperty("@class"));
						return map;
					})
					.collect(Collectors.toCollection(LinkedList::new));

			return new GraphContent(vertices, edges);
		} finally {
			if (orientGraph != null) {
				orientGraph.shutdown();
			}
		}

	}
