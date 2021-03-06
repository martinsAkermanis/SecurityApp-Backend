package io.codelex.securityapp.route;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.codelex.securityapp.Password;
import io.codelex.securityapp.api.AddIncidentRequest;
import io.codelex.securityapp.repository.RepositoryUnitService;
import io.codelex.securityapp.repository.SimpleNearestUnitService;
import io.codelex.securityapp.repository.UnitRepository;
import io.codelex.securityapp.repository.models.Client;
import io.codelex.securityapp.repository.models.Incident;
import io.codelex.securityapp.repository.models.Unit;
import org.junit.Rule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class SimpleNearestUnitServiceTest {

    private UnitRepository repository = Mockito.mock(UnitRepository.class);
    private RepositoryUnitService repositoryUnitService;
    private SimpleNearestUnitService nearestUnit;
    private Password encoder;
    
    @Rule
    static WireMockRule wireMock = new WireMockRule();
    private RouteGateway routeGateway;
    

    @BeforeAll
    static void setUpOnce() {
        wireMock.start();
    }

    @BeforeEach
    void setUp() {
        GoogleMapsProps props = new GoogleMapsProps();
        props.setApiUrl("http://localhost:" + wireMock.port());
        routeGateway = new RouteGateway(props);
        repositoryUnitService = new RepositoryUnitService(encoder, repository);
        nearestUnit = new SimpleNearestUnitService(repositoryUnitService, routeGateway);
    }

    @Test
    void should_return_nearest_unit() throws Exception {
        //given

        Unit closestUnit = new Unit(
                //Vansu tilts
                "John@Doe.com", "123", new BigDecimal(56.952092).setScale(6, RoundingMode.DOWN),
                new BigDecimal(24.099975).setScale(6, RoundingMode.DOWN),
                true
        );

        Unit farthestUnit = new Unit(
                //Maskachka
                "John@Doe.com", "123", new BigDecimal(56.940931).setScale(6, RoundingMode.DOWN),
                new BigDecimal(24.137081).setScale(6, RoundingMode.DOWN),
                true
        );

        AddIncidentRequest request = new AddIncidentRequest(
                "john@doe.com", new BigDecimal(24.941887).setScale(6, RoundingMode.DOWN),
                new BigDecimal(56.095740).setScale(6, RoundingMode.DOWN)
        );

        List<Unit> unitList = new ArrayList<>();
        unitList.add(closestUnit);
        unitList.add(farthestUnit);

        String closestLocation = closestUnit.getLatitude().toString() + "," + closestUnit.getLongitude().toPlainString();
        String farthestLocation = farthestUnit.getLatitude().toString() + "," + farthestUnit.getLongitude().toPlainString();

        //when
        Mockito.when(repository.save(any()))
                .thenAnswer((Answer) invocation -> invocation.getArguments()[0]);

        when(repository.searchAvailable()).thenReturn((unitList));

        File fileClosest = ResourceUtils.getFile(this.getClass().getResource("/stubs/closest.json"));
        Assertions.assertTrue(fileClosest.exists());

        File fileFarthest = ResourceUtils.getFile(this.getClass().getResource("/stubs/farthest.json"));
        Assertions.assertTrue(fileFarthest.exists());

        byte[] closestUnitFile = Files.readAllBytes(fileClosest.toPath());
        byte[] farthestUnitFile = Files.readAllBytes(fileFarthest.toPath());

        wireMock.stubFor(get(urlPathEqualTo("/maps/api/distancematrix/json"))
                .withQueryParam("destinations", equalTo(closestLocation))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json; charset=utf-8")
                        .withStatus(200)
                        .withBody(closestUnitFile)));

        wireMock.stubFor(get(urlPathEqualTo("/maps/api/distancematrix/json"))
                .withQueryParam("destinations", equalTo(farthestLocation))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json; charset=utf-8")
                        .withStatus(200)
                        .withBody(farthestUnitFile)));

        //then
        Unit closestUnitFound = nearestUnit.searchNearestUnit(request);
        Assertions.assertEquals(closestUnit, closestUnitFound);
        Assertions.assertNotNull(closestUnitFound);
        Assertions.assertEquals(closestUnit.getLatitude(), nearestUnit.searchNearestUnit(request).getLatitude());
    }
}