/**
 * Created by Luzius Meisser on 2021-03-10
 * Copyright: Aktionariat AG, Zurich
 * Contact: luzius@aktionariat.com
 *
 * Feel free to reuse this code under the MIT License
 * https://opensource.org/licenses/MIT
 */
package com.aktionariat.bridge;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class WalletConnectMessage {

	public static final ObjectMapper MAPPER = constructMapper(false);

	public boolean silent;
	public String topic;
	public String payload;
	public String type;

	public static WalletConnectMessage parse(String content) throws IOException {
		try {
			return MAPPER.readValue(content, WalletConnectMessage.class);
		} catch (JacksonException e) {
			throw new IOException(e);
		}
	}

	public String getJson() {
		try {
			return MAPPER.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	private static ObjectMapper constructMapper(boolean failOnUnknown) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
		mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
		mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, failOnUnknown);
		mapper.setSerializationInclusion(Include.NON_NULL);
		return mapper;
	}

}
