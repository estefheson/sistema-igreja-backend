package com.igreja.system.listmodule.service;

import com.igreja.system.listmodule.dto.admin.ChurchListManagementItemResponse;
import com.igreja.system.listmodule.dto.admin.ChurchListManagementResponse;
import com.igreja.system.listmodule.entity.ChurchList;
import com.igreja.system.listmodule.entity.ChurchListItem;
import com.igreja.system.listmodule.entity.PublicListSubmission;
import com.igreja.system.listmodule.entity.PublicListSubmissionItem;
import com.igreja.system.listmodule.repository.ChurchListRepository;
import com.igreja.system.listmodule.repository.PublicListSubmissionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ChurchListAdminServiceIntegrationTest {

    @Autowired
    private ChurchListAdminService churchListAdminService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChurchListRepository churchListRepository;

    @Autowired
    private PublicListSubmissionRepository publicListSubmissionRepository;

    private final List<Long> createdSubmissionIds = new ArrayList<>();
    private final List<Long> createdListIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();

        if (!createdSubmissionIds.isEmpty()) {
            publicListSubmissionRepository.deleteAllById(createdSubmissionIds);
            createdSubmissionIds.clear();
        }

        if (!createdListIds.isEmpty()) {
            churchListRepository.deleteAllById(createdListIds);
            createdListIds.clear();
        }
    }

    @Test
    void shouldReturnCalculatedStatusesForAdministrativeListView() {
        authenticateAsAdmin();
        long suffix = System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();

        ChurchList activeList = createList(
                "Lista ativa " + suffix,
                true,
                now.minusDays(1),
                now.plusDays(1),
                List.of()
        );
        ChurchList scheduledList = createList(
                "Lista agendada " + suffix,
                true,
                now.plusDays(1),
                now.plusDays(2),
                List.of()
        );
        ChurchList endedList = createList(
                "Lista encerrada " + suffix,
                true,
                now.minusDays(3),
                now.minusDays(1),
                List.of()
        );
        ChurchList inactiveList = createList(
                "Lista inativa " + suffix,
                false,
                now.minusDays(1),
                now.plusDays(1),
                List.of()
        );

        Map<Long, String> statusById = churchListAdminService.findAll().stream()
                .filter(list -> createdListIds.contains(list.id()))
                .collect(LinkedHashMap::new, (map, list) -> map.put(list.id(), list.status()), LinkedHashMap::putAll);

        assertThat(statusById)
                .containsEntry(activeList.getId(), "Ativa")
                .containsEntry(scheduledList.getId(), "Agendada")
                .containsEntry(endedList.getId(), "Encerrada")
                .containsEntry(inactiveList.getId(), "Inativa");
    }

    @Test
    void shouldReturnManagementMetricsAndParticipantsFromPersistedSubmissions() {
        authenticateAsAdmin();
        long suffix = System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();

        ChurchList list = createList(
                "Lista acompanhamento " + suffix,
                true,
                now.minusDays(1),
                now.plusDays(1),
                List.of(
                        buildItem("Arroz", 5),
                        buildItem("Feijao", 3)
                )
        );

        ChurchListItem rice = findItemByName(list, "Arroz");
        ChurchListItem beans = findItemByName(list, "Feijao");

        createSubmission(list, "Maria da Silva", "11999990001", Map.of(rice, 2, beans, 1));
        createSubmission(list, "Jose Santos", "11999990002", Map.of(rice, 2));

        ChurchListManagementResponse response = churchListAdminService.findManagementById(list.getId());

        assertThat(response.status()).isEqualTo("Ativa");
        assertThat(response.totalItems()).isEqualTo(2);
        assertThat(response.totalReserved()).isEqualTo(5);
        assertThat(response.totalAvailable()).isEqualTo(3);
        assertThat(response.totalParticipants()).isEqualTo(2);
        assertThat(response.fillPercentage()).isEqualTo(62.5);

        ChurchListManagementItemResponse riceResponse = findManagementItemByName(response, "Arroz");
        assertThat(riceResponse.reservedQuantity()).isEqualTo(4);
        assertThat(riceResponse.availableQuantity()).isEqualTo(1);
        assertThat(riceResponse.fillPercentage()).isEqualTo(80.0);
        assertThat(riceResponse.participants()).hasSize(2);
        assertThat(riceResponse.participants())
                .extracting(participant -> participant.fullName(), participant -> participant.phone(), participant -> participant.quantity())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Maria da Silva", "11999990001", 2),
                        org.assertj.core.groups.Tuple.tuple("Jose Santos", "11999990002", 2)
                );
        assertThat(riceResponse.participants())
                .allSatisfy(participant -> assertThat(participant.createdAt()).isNotNull());

        ChurchListManagementItemResponse beansResponse = findManagementItemByName(response, "Feijao");
        assertThat(beansResponse.reservedQuantity()).isEqualTo(1);
        assertThat(beansResponse.availableQuantity()).isEqualTo(2);
        assertThat(beansResponse.fillPercentage()).isEqualTo(33.33);
        assertThat(beansResponse.participants()).hasSize(1);
        assertThat(beansResponse.participants().getFirst().fullName()).isEqualTo("Maria da Silva");
    }

    @Test
    void shouldReturnConsistentJsonForAdministrativeManagementScenarios() throws Exception {
        long suffix = System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();

        ChurchList list = createList(
                "Lista json " + suffix,
                true,
                now.minusDays(1),
                now.plusDays(1),
                List.of(
                        buildItem("Sem reserva", 3),
                        buildItem("Reserva parcial", 3),
                        buildItem("Reserva total", 3)
                )
        );

        ChurchListItem noReservationItem = findItemByName(list, "Sem reserva");
        ChurchListItem partialReservationItem = findItemByName(list, "Reserva parcial");
        ChurchListItem fullReservationItem = findItemByName(list, "Reserva total");

        createSubmission(list, "Participante Parcial", "11999990100", Map.of(partialReservationItem, 1));
        createSubmission(list, "Participante Total", "11999990200", Map.of(fullReservationItem, 3));

        mockMvc.perform(get("/api/lists/{id}/management", list.getId())
                        .with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.totalReserved").value(4))
                .andExpect(jsonPath("$.totalAvailable").value(5))
                .andExpect(jsonPath("$.totalParticipants").value(2))
                .andExpect(jsonPath("$.fillPercentage").value(44.44))
                .andExpect(jsonPath("$.items[0].id").value(noReservationItem.getId()))
                .andExpect(jsonPath("$.items[0].totalQuantity").value(3))
                .andExpect(jsonPath("$.items[0].reservedQuantity").value(0))
                .andExpect(jsonPath("$.items[0].availableQuantity").value(3))
                .andExpect(jsonPath("$.items[0].fillPercentage").value(0.0))
                .andExpect(jsonPath("$.items[0].participants.length()").value(0))
                .andExpect(jsonPath("$.items[1].id").value(partialReservationItem.getId()))
                .andExpect(jsonPath("$.items[1].totalQuantity").value(3))
                .andExpect(jsonPath("$.items[1].reservedQuantity").value(1))
                .andExpect(jsonPath("$.items[1].availableQuantity").value(2))
                .andExpect(jsonPath("$.items[1].fillPercentage").value(33.33))
                .andExpect(jsonPath("$.items[1].participants[0].fullName").value("Participante Parcial"))
                .andExpect(jsonPath("$.items[1].participants[0].phone").value("11999990100"))
                .andExpect(jsonPath("$.items[1].participants[0].quantity").value(1))
                .andExpect(jsonPath("$.items[1].participants[0].createdAt").isNotEmpty())
                .andExpect(jsonPath("$.items[2].id").value(fullReservationItem.getId()))
                .andExpect(jsonPath("$.items[2].totalQuantity").value(3))
                .andExpect(jsonPath("$.items[2].reservedQuantity").value(3))
                .andExpect(jsonPath("$.items[2].availableQuantity").value(0))
                .andExpect(jsonPath("$.items[2].fillPercentage").value(100.0))
                .andExpect(jsonPath("$.items[2].participants[0].fullName").value("Participante Total"))
                .andExpect(jsonPath("$.items[2].participants[0].phone").value("11999990200"))
                .andExpect(jsonPath("$.items[2].participants[0].quantity").value(3))
                .andExpect(jsonPath("$.items[2].participants[0].createdAt").isNotEmpty());
    }

    @Test
    void shouldClampReservedQuantityWhenPersistedDataExceedsItemTotalQuantity() {
        authenticateAsAdmin();
        long suffix = System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();

        ChurchList list = createList(
                "Lista sem clamp " + suffix,
                true,
                now.minusDays(1),
                now.plusDays(1),
                List.of(buildItem("Cesta basica", 3))
        );

        ChurchListItem item = findItemByName(list, "Cesta basica");

        createSubmission(list, "Ana Paula", "11999990003", Map.of(item, 2));
        createSubmission(list, "Carlos Lima", "11999990004", Map.of(item, 3));

        ChurchListManagementResponse response = churchListAdminService.findManagementById(list.getId());
        ChurchListManagementItemResponse itemResponse = response.items().getFirst();

        assertThat(response.totalReserved()).isEqualTo(3);
        assertThat(response.totalAvailable()).isZero();
        assertThat(response.fillPercentage()).isEqualTo(100.0);
        assertThat(itemResponse.reservedQuantity()).isEqualTo(3);
        assertThat(itemResponse.availableQuantity()).isZero();
        assertThat(itemResponse.fillPercentage()).isEqualTo(100.0);
    }

    private ChurchList createList(
            String name,
            boolean active,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            List<ChurchListItem> items
    ) {
        ChurchList list = ChurchList.builder()
                .name(name)
                .description("Descricao " + name)
                .active(active)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .build();

        items.forEach(list::addItem);

        ChurchList savedList = churchListRepository.saveAndFlush(list);
        createdListIds.add(savedList.getId());
        return savedList;
    }

    private ChurchListItem buildItem(String name, int totalQuantity) {
        return ChurchListItem.builder()
                .name(name)
                .description("Item " + name)
                .totalQuantity(totalQuantity)
                .build();
    }

    private void createSubmission(
            ChurchList list,
            String fullName,
            String phone,
            Map<ChurchListItem, Integer> quantitiesByItem
    ) {
        PublicListSubmission submission = PublicListSubmission.builder()
                .list(list)
                .fullName(fullName)
                .phone(phone)
                .build();

        LinkedHashSet<PublicListSubmissionItem> items = new LinkedHashSet<>();

        quantitiesByItem.forEach((item, quantity) -> items.add(PublicListSubmissionItem.builder()
                .submission(submission)
                .listItem(item)
                .quantity(quantity)
                .build()));

        submission.setItems(items);

        PublicListSubmission savedSubmission = publicListSubmissionRepository.saveAndFlush(submission);
        createdSubmissionIds.add(savedSubmission.getId());
    }

    private ChurchListItem findItemByName(ChurchList list, String itemName) {
        return list.getItems().stream()
                .filter(item -> itemName.equals(item.getName()))
                .findFirst()
                .orElseThrow();
    }

    private ChurchListManagementItemResponse findManagementItemByName(
            ChurchListManagementResponse response,
            String itemName
    ) {
        return response.items().stream()
                .filter(item -> itemName.equals(item.name()))
                .findFirst()
                .orElseThrow();
    }

    private void authenticateAsAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );
    }
}
