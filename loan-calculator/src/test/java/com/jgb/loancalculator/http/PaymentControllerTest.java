package com.jgb.loancalculator.http;

import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNull.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@WebAppConfiguration
@SpringBootTest
public class PaymentControllerTest {

    private MockMvc mockMvc;
    
    @Autowired
    private WebApplicationContext webApplicationContext;
    
    @Before
    public void setup() {
        mockMvc = webAppContextSetup(webApplicationContext).build();
    }
    
    @Test
    public void testNoParameters() throws Exception {
        mockMvc.perform(get("/payment")).andExpect(status().is(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    public void testNoInterest() throws Exception {
        mockMvc.perform(get("/resetCount")).andExpect(status().is(HttpStatus.OK.value()));

        mockMvc.perform(get("/payment?amount=180000&rate=0&years=30"))
        .andExpect(status().is(HttpStatus.OK.value()))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
        .andExpect(jsonPath("$.amount", is(180000.0)))
        .andExpect(jsonPath("$.years", is(30)))
        .andExpect(jsonPath("$.rate", is(0.0)))
        .andExpect(jsonPath("$.payment", is(500.0)))
        .andExpect(jsonPath("$.instance", is(nullValue())))
        .andExpect(jsonPath("$.count", is(1)));
    }

    @Test
    public void testRegularInterest() throws Exception {
        mockMvc.perform(get("/resetCount")).andExpect(status().is(HttpStatus.OK.value()));

        mockMvc.perform(get("/payment?amount=200000&rate=6.5&years=30"))
        .andExpect(status().is(HttpStatus.OK.value()))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
        .andExpect(jsonPath("$.amount", is(200000.0)))
        .andExpect(jsonPath("$.years", is(30)))
        .andExpect(jsonPath("$.rate", is(6.5)))
        .andExpect(jsonPath("$.payment", is(1264.14)))
        .andExpect(jsonPath("$.instance", is(nullValue())))
        .andExpect(jsonPath("$.count", is(1)));
    }

    @Test
    public void testThatCounterDoesIncrement() throws Exception {
        mockMvc.perform(get("/resetCount")).andExpect(status().is(HttpStatus.OK.value()));
        
        mockMvc.perform(get("/payment?amount=180000&rate=0&years=30"))
        .andExpect(status().is(HttpStatus.OK.value()))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
        .andExpect(jsonPath("$.amount", is(180000.0)))
        .andExpect(jsonPath("$.years", is(30)))
        .andExpect(jsonPath("$.rate", is(0.0)))
        .andExpect(jsonPath("$.payment", is(500.0)))
        .andExpect(jsonPath("$.instance", is(nullValue())))
        .andExpect(jsonPath("$.count", is(1)));

        mockMvc.perform(get("/payment?amount=200000&rate=6.5&years=30"))
        .andExpect(status().is(HttpStatus.OK.value()))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
        .andExpect(jsonPath("$.amount", is(200000.0)))
        .andExpect(jsonPath("$.years", is(30)))
        .andExpect(jsonPath("$.rate", is(6.5)))
        .andExpect(jsonPath("$.payment", is(1264.14)))
        .andExpect(jsonPath("$.instance", is(nullValue())))
        .andExpect(jsonPath("$.count", is(2)));
    }
}
