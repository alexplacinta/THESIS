package md.openmoney.rest.controller;


import java.util.*;

import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import md.openmoney.rest.dao.ContractStorage;
import md.openmoney.rest.dao.ElementStorage;
import md.openmoney.rest.dao.InstitutionStorage;
import md.openmoney.rest.dto.GraphContent;
import md.openmoney.rest.dto.graph.ContractEdgeDTO;
import md.openmoney.rest.dto.graph.EdgeDTO;
import md.openmoney.rest.dto.graph.VertexDTO;
import md.openmoney.rest.service.GraphProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
import com.tinkerpop.pipes.branch.LoopPipe;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(description = "for graph processing")
@RestController
@RequestMapping(path = "/graph")
public class GraphProcessingController {
    @Autowired
    private ElementStorage elementStorage;
    @Autowired
    private InstitutionStorage institutionStorage;
    @Autowired
    private ContractStorage contractStorage;
    @Autowired
    private GraphProcessingService graphProcessingService;

    @ApiOperation(value = "(DONE) get first level graph from rid")
    @RequestMapping(value = "/{id:\\d+\\:\\d+}/firstLevel", method = RequestMethod.GET)
    public ResponseEntity<Object> getFirstLevelGraph(@PathVariable("id") String entityRid) {

        GraphContent result = elementStorage.getFirstLevelGraphOfEntity(entityRid);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @ApiOperation(value = "(DONE) get relational bidirectional graph from rid entity")
    @RequestMapping(value = "/{id:\\d+\\:\\d+}/relationalBidirectional", method = RequestMethod.GET)
    public ResponseEntity<Object> getRelationalBidirectionalGraph(@PathVariable("id") String entityRid,
                                                                  @RequestParam("depth") int depth) {

        if (depth > 8 || depth < 1) return new ResponseEntity<>(HttpStatus.PRECONDITION_FAILED);

        GraphContent result = elementStorage.getRelationalBidirectionalGraphOfEntity(entityRid, depth);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @ApiOperation(value = "(DONE) get connection graph with schema between personRid and institutionRid, set maxDepth >= 2")
    @RequestMapping(value = "/connectionWithSchema", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
    public ResponseEntity<Object> getConnectionGraphWithSchema(@RequestParam("personRid") String personRid,
                                                               @RequestParam("institutionRid") String institutionRid,
                                                               @RequestParam(value = "maxDepth", required = false) Integer maxDepth) {

        if (maxDepth != null && maxDepth <= 1)
            return new ResponseEntity<>(HttpStatus.PRECONDITION_FAILED);

        String paths = graphProcessingService.getPathsOfConnectionGraph(personRid, institutionRid, maxDepth);

        return new ResponseEntity<>(paths, HttpStatus.OK);
    }
}
