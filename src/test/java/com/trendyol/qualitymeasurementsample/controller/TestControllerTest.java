package com.trendyol.qualitymeasurementsample.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(TestController.class)
class TestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void it_should_get_test_message() throws Exception {
        //Given

        //When
        var result = mockMvc.perform(get("/test"));

        //Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Hello Sonar!"));
    }

    @Test
    void it_should_sum_two_numbers() throws Exception {
        //Given
        int a = 5;
        int b = 10;

        //When
        ResultActions result = mockMvc.perform(get("/test/sum")
                .param("a", String.valueOf(a))
                .param("b", String.valueOf(b)));

        //Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$").value(15));
    }
}