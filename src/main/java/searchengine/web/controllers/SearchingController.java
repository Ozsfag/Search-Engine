package searchengine.web.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.services.searching.SearchingService;
import searchengine.web.models.TotalSearchResponse;
import searchengine.web.models.UpsertSearchRequest;

@RestController
@RequestMapping("/searching")
@Tag(name = "Searching controller v1", description = "Operations related to searching")
public class SearchingController {

  @Autowired private SearchingService searchingService;

  /**
   * Performs a search based on the provided query, site (optional), offset, and limit.
   *
   * @return a ResponseEntity containing the search results
   */
  @GetMapping("/search")
  @Operation(summary = "Search", description = "Performs a search based on query parameters.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        content =
            @Content(
                schema = @Schema(implementation = TotalSearchResponse.class),
                mediaType = "application/json")),
    @ApiResponse(
        responseCode = "404",
        content =
            @Content(
                schema = @Schema(implementation = TotalSearchResponse.class),
                mediaType = "application/json"))
  })
  public ResponseEntity<TotalSearchResponse> search(
      @ModelAttribute UpsertSearchRequest upsertSearchRequest) {
    return ResponseEntity.ok(searchingService.search(upsertSearchRequest));
  }
}
