/*
 * Copyright (c) 2020 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.exadel.frs.controller;

import com.exadel.frs.commonservice.entity.Model;
import com.exadel.frs.dto.ui.ModelCreateDto;
import com.exadel.frs.dto.ui.ModelResponseDto;
import com.exadel.frs.dto.ui.ModelUpdateDto;
import com.exadel.frs.mapper.MlModelMapper;
import com.exadel.frs.service.ModelService;
import com.exadel.frs.system.security.config.AuthServerConfig;
import com.exadel.frs.system.security.config.ResourceServerConfig;
import com.exadel.frs.system.security.config.WebSecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.exadel.frs.utils.TestUtils.buildUser;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ModelController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {WebSecurityConfig.class, AuthServerConfig.class, ResourceServerConfig.class}
        )
)
class ModelControllerTest {

    @MockBean
    private ModelService modelService;

    @MockBean
    private MlModelMapper modelMapper;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private MockMvc mockMvc;

    private static final String APP_GUID = "app-guid";
    private static final String MODEL_GUID = "model-guid";
    private static final String MODEL_NAME = "model-name";

    @Test
    void shouldReturnMessageAndCodeWhenModelNameIsMissingOnUpdate() throws Exception {
        val expectedContent = "{\"message\":\"Model name cannot be empty\",\"code\":26}";
        val bodyWithEmptyName = new ModelUpdateDto();
        bodyWithEmptyName.setName(null);

        val bodyWithNoName = new ModelUpdateDto();

        val updateRequest = put("/app/" + APP_GUID + "/model/" + MODEL_GUID)
                .with(csrf())
                .with(user(buildUser()))
                .contentType(APPLICATION_JSON);

        mockMvc.perform(updateRequest.content(mapper.writeValueAsString(bodyWithEmptyName)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(expectedContent));

        mockMvc.perform(updateRequest.content(mapper.writeValueAsString(bodyWithNoName)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(expectedContent));
    }

    @Test
    void shouldReturnErrorMessageWhenNameIsMissingOnCreateNewModel() throws Exception {
        val bodyWithEmptyName = new ModelCreateDto();
        bodyWithEmptyName.setName("");
        bodyWithEmptyName.setType("RECOGNITION");

        val bodyWithNoName = new ModelCreateDto();
        bodyWithNoName.setType("RECOGNITION");

        val createNewModelRequest = post("/app/" + APP_GUID + "/model")
                .with(csrf())
                .with(user(buildUser()))
                .contentType(APPLICATION_JSON);

        mockMvc.perform(createNewModelRequest.content(mapper.writeValueAsString(bodyWithEmptyName)))
               .andExpect(status().isBadRequest())
               .andExpect(content().string("{\"message\":\"Model name size must be between 1 and 50\",\"code\":26}"));

        mockMvc.perform(createNewModelRequest.content(mapper.writeValueAsString(bodyWithNoName)))
               .andExpect(status().isBadRequest())
               .andExpect(content().string("{\"message\":\"Model name cannot be empty\",\"code\":26}"));
    }

    @Test
    void shouldReturnModel() throws Exception {
        val request = get("/app/" + APP_GUID + "/model/" + MODEL_GUID)
                .with(csrf())
                .with(user(buildUser()))
                .contentType(APPLICATION_JSON);

        val responseDto = new ModelResponseDto();
        responseDto.setName(MODEL_NAME);

        when(modelService.getModelDto(eq(APP_GUID), eq(MODEL_GUID), anyLong())).thenReturn(responseDto);

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(content().string(mapper.writeValueAsString(responseDto)));
    }

    @Test
    void shouldReturnModels() throws Exception {
        val request = get("/app/" + APP_GUID + "/models")
                .with(csrf())
                .with(user(buildUser()))
                .contentType(APPLICATION_JSON);

        val responseDto = new ModelResponseDto();
        responseDto.setName(MODEL_NAME);

        when(modelService.getModels(eq(APP_GUID), anyLong())).thenReturn(List.of(responseDto, responseDto));

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(content().string(mapper.writeValueAsString(List.of(responseDto, responseDto))));
    }

    @Test
    void shouldReturnCreatedModel() throws Exception {
        val createDto = new ModelCreateDto();
        createDto.setName(MODEL_NAME);
        createDto.setType("RECOGNITION");

        val createRequest = post("/app/" + APP_GUID + "/model")
                .with(csrf())
                .with(user(buildUser()))
                .contentType(APPLICATION_JSON)
                .content(mapper.writeValueAsString(createDto));

        val model = Model.builder()
                .name(MODEL_NAME)
                .build();

        val responseDto = new ModelResponseDto();
        responseDto.setName(MODEL_NAME);

        when(modelService.createRecognitionModel(any(ModelCreateDto.class), eq(APP_GUID), anyLong())).thenReturn(model);
        when(modelMapper.toResponseDto(any(Model.class), eq(APP_GUID))).thenReturn(responseDto);

        mockMvc.perform(createRequest)
                .andExpect(status().isCreated())
                .andExpect(content().string(mapper.writeValueAsString(responseDto)));
    }

    @Test
    void shouldReturnUpdatedModel() throws Exception {
        val updateDto = new ModelUpdateDto();
        updateDto.setName(MODEL_NAME);

        val createRequest = put("/app/" + APP_GUID + "/model/" + MODEL_GUID)
                .with(csrf())
                .with(user(buildUser()))
                .contentType(APPLICATION_JSON)
                .content(mapper.writeValueAsString(updateDto));

        val model = Model.builder()
                .name(MODEL_NAME)
                .build();

        val responseDto = new ModelResponseDto();
        responseDto.setName(MODEL_NAME);

        when(modelService.updateModel(any(ModelUpdateDto.class), eq(APP_GUID), eq(MODEL_GUID), anyLong())).thenReturn(model);
        when(modelMapper.toResponseDto(any(Model.class), eq(APP_GUID))).thenReturn(responseDto);

        mockMvc.perform(createRequest)
                .andExpect(status().isOk())
                .andExpect(content().string(mapper.writeValueAsString(responseDto)));
    }

    @Test
    void shouldReturnUpdatedWithApiKeyModel() throws Exception {
        val updateDto = new ModelUpdateDto();
        updateDto.setName(MODEL_NAME);

        val request = put("/app/" + APP_GUID + "/model/" + MODEL_GUID + "/apikey")
                .with(csrf())
                .with(user(buildUser()))
                .contentType(APPLICATION_JSON);

        val newApiKey = randomUUID().toString();

        val responseDto = new ModelResponseDto();
        responseDto.setApiKey(newApiKey);

        when(modelService.getModelDto(eq(APP_GUID), eq(MODEL_GUID), anyLong())).thenReturn(responseDto);

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(content().string(mapper.writeValueAsString(responseDto)));
    }

    @Test
    void shouldReturnOkWhenDeleteModel() throws Exception {
        val updateDto = new ModelUpdateDto();
        updateDto.setName(MODEL_NAME);

        val request = delete("/app/" + APP_GUID + "/model/" + MODEL_GUID)
                .with(csrf())
                .with(user(buildUser()))
                .contentType(APPLICATION_JSON);

        doNothing().when(modelService).deleteModel(eq(APP_GUID), eq(MODEL_GUID), anyLong());

        mockMvc.perform(request)
                .andExpect(status().isOk());
    }
}