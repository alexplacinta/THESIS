package md.openmoney.rest.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import md.openmoney.rest.dao.ContractStorage;
import md.openmoney.rest.dao.ElementStorage;
import md.openmoney.rest.dao.InstitutionStorage;
import md.openmoney.rest.dto.graph.ContractEdgeDTO;
import md.openmoney.rest.dto.graph.EdgeDTO;
import md.openmoney.rest.dto.graph.VertexDTO;
import org.springframework.cache.annotation.Cacheable;

import java.util.*;

public class GraphProcessingService {

    private ElementStorage elementStorage;
    private InstitutionStorage institutionStorage;
    private ContractStorage contractStorage;
    private ObjectMapper objectMapper = new ObjectMapper();

    public GraphProcessingService(){}

    public GraphProcessingService(ElementStorage elementStorage,
                                  InstitutionStorage institutionStorage,
                                  ContractStorage contractStorage) {
        this.elementStorage = elementStorage;
        this.institutionStorage = institutionStorage;
        this.contractStorage = contractStorage;
    }

//    @Cacheable(value = "pathsOfConnectionGraph", key="#personRid + #institutionRid")
    public String getPathsOfConnectionGraph(String personRid,
                                            String institutionRid,
                                            Integer maxDepth) {
        // check if exists agregated connection
        Map<String, Object> agregatedConnection = elementStorage.getAgregatedConnection(personRid, institutionRid);

        String pathsResult = null;

        if (agregatedConnection != null) {
            pathsResult = agregatedConnection.get("pathsResult").toString();
        } else {
            List<String> companyRidsWhichHaveContractWithInstitution = elementStorage
                    .getCompanyRidsWhichHaveContractWithInstitution(institutionRid);

            List<Map<String, Object>> paths = new ArrayList<>();

            companyRidsWhichHaveContractWithInstitution
                    .forEach(companyRid -> {
                        List<VertexDTO> shortestPath = elementStorage.getShortestPath(
                                personRid, companyRid.substring(1), maxDepth);

                        if (shortestPath == null || shortestPath.size() < 2) return;

                        // start graph
                        Set<VertexDTO> vertices = new HashSet<>();
                        Set<EdgeDTO> edges = new HashSet<>();

                        // add last vertex first of all
                        vertices.add(shortestPath.get(shortestPath.size() - 1));

                        for (int i = 0; i < shortestPath.size() - 1; i++) {
                            vertices.add(shortestPath.get(i));

                            String firstRid = shortestPath.get(i).getId().substring(1);
                            String secondRid = shortestPath.get(i + 1).getId().substring(1);

                            List<EdgeDTO> betweenEdges = elementStorage.getEdgesBetween(firstRid, secondRid);
                            edges.addAll(betweenEdges);
                        }

                        EdgeDTO contractEdge = new ContractEdgeDTO(
                                companyRid + "-#" + institutionRid, "Contract",
                                companyRid, "#" + institutionRid,
                                (int)contractStorage.getSumOfContractsBetweenCompanyAndInstitution(
                                        companyRid.substring(1), institutionRid
                                )
                        );
                        edges.add(contractEdge);

                        Map<String, Object> institutionInfo = institutionStorage.getInstitutionById(institutionRid);
                        VertexDTO institution = new VertexDTO("#" + institutionRid, "Institution", institutionInfo.get("name").toString().replace("\"", "-"));
                        vertices.add(institution);

                        // end graph
                        Map<String, Object> graph = new HashMap<>();
                        graph.put("vertices", vertices);
                        graph.put("edges", edges);

                        // append graph
                        Map<String, Object> path = new HashMap<>();
                        path.put("depth", shortestPath.size());
                        path.put("graph", graph);
                        paths.add(path);
                    });
            Collections.sort(paths, (p1, p2) -> ((Integer)p1.get("depth")).compareTo(((Integer)p2.get("depth"))));

            try {
                pathsResult = objectMapper.writeValueAsString(paths);
                System.out.println(pathsResult);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            elementStorage.saveAgregatedConnection(personRid, institutionRid, pathsResult);

        }
        return pathsResult;
    }
}
