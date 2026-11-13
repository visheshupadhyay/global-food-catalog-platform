package com.vishesh.foodcatalog.catalog.api;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.vishesh.foodcatalog.catalog.application.MenuItemScheduleService;
import com.vishesh.foodcatalog.catalog.domain.MenuItemSchedule;

@WebMvcTest(MenuItemScheduleController.class)
class MenuItemScheduleControllerMvcTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MenuItemScheduleService service;

    @Test
    void createSchedule_returns201AndPassesOutletScope() throws Exception {
        when(service.save(eq(101L), org.mockito.ArgumentMatchers.any(CreateScheduleRequest.class)))
                .thenReturn(new MenuItemSchedule(101L, 501L, Map.of()));

        mockMvc.perform(post("/api/v1/menu-items/101/schedules")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"outletId":501,"windows":{"MONDAY":[{"startTime":"09:00:00","endTime":"12:00:00"}]}}
                        """))
                .andExpect(status().isCreated());

        verify(service).save(eq(101L), org.mockito.ArgumentMatchers.any(CreateScheduleRequest.class));
    }
}
