package com.shepherdmoney.interviewproject;

import com.shepherdmoney.interviewproject.controller.CreditCardController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@AutoConfigureMockMvc
class InterviewProjectApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CreditCardController creditCardController;

    @Test
    void contextLoads() {
    }

    private int createNewUser(String name, String email) throws Exception {
        String jsonContent = String.format("{\"name\":\"%s\", \"email\":\"%s\"}", name, email);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.put("/user")
                        .content(jsonContent)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        return Integer.parseInt(responseBody);
    }

    @Test
    void testCreateAndRetrieveUser() throws Exception {
        // Send PUT request to create a new user
        int userId = this.createNewUser("Test Insert", "testinsert@gmail.com");

        // Send GET request to retrieve the user's information
        mockMvc.perform(MockMvcRequestBuilders.get("/user")
                        .param("userId", String.valueOf(userId)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json("{\"id\":" + userId + "," +
                        "\"name\":\"Test Insert\"," +
                        "\"email\":\"testinsert@gmail.com\"}"));
    }

    @Test
    void testDeleteUser() throws Exception {
        // Send PUT request to create a new user
        int userId = this.createNewUser("Test Delete", "testdelete@gmail.com");

        // Send DELETE request to delete the user
        mockMvc.perform(MockMvcRequestBuilders.delete("/user")
                        .param("userId", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }


}
