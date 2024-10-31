package com.declspecl.controller.responses;

import com.declspecl.model.PersonaName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@Value.Style(init = "with*", get = { "is*", "get*" })
@JsonSerialize(as = ImmutableGetUserGuessesResponse.class)
public abstract class GetUserGuessesResponse {
	public abstract PersonaName todayPersona();
	public abstract List<String> guesses();
}
