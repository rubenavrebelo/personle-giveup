package com.declspecl.controller;

import com.declspecl.PersonleApiConstants;
import com.declspecl.components.ControllerUtils;
import com.declspecl.components.UserSessionGenerator;
import com.declspecl.components.UserSessionTransformer;
import com.declspecl.controller.requests.PostUserGuessRequest;
import com.declspecl.controller.responses.GetUserGuessesResponse;
import com.declspecl.controller.responses.ImmutableGetUserGuessesResponse;
import com.declspecl.model.EncodedHashedUserSessionId;
import com.declspecl.model.HashedUserSessionId;
import com.declspecl.model.ImmutableDailyGuesses;
import com.declspecl.model.PersonaName;
import com.declspecl.repository.DailyGuessesRepository;
import com.declspecl.model.DailyGuesses;
import com.declspecl.repository.DailyPersonaRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Log4j2
@Controller
public class DailyGuessesController {
	private final ControllerUtils controllerUtils;
	private final Supplier<LocalDate> todaySupplier;
	private final UserSessionGenerator userSessionGenerator;
	private final DailyPersonaRepository dailyPersonaRepository;
	private final DailyGuessesRepository dailyGuessesRepository;
	private final UserSessionTransformer userSessionTransformer;

	@Autowired
	public DailyGuessesController(
			ControllerUtils controllerUtils,
			Supplier<LocalDate> todaySupplier,
			UserSessionGenerator userSessionGenerator,
			DailyPersonaRepository dailyPersonaRepository,
			DailyGuessesRepository dailyGuessesRepository,
			UserSessionTransformer userSessionTransformer
	) {
		this.todaySupplier = todaySupplier;
		this.controllerUtils = controllerUtils;
		this.userSessionGenerator = userSessionGenerator;
		this.dailyPersonaRepository = dailyPersonaRepository;
		this.dailyGuessesRepository = dailyGuessesRepository;
		this.userSessionTransformer = userSessionTransformer;
	}

	@GetMapping("/api/daily/guess")
	public ResponseEntity<GetUserGuessesResponse> getUserGuessesToday(HttpServletRequest request) throws ExecutionException {
		Map<String, String> cookies = controllerUtils.buildCookieMap(request);
		Optional<EncodedHashedUserSessionId> userSessionCookie = controllerUtils.getUserSessionCookie(cookies);

		PersonaName todayPersona = dailyPersonaRepository.getPersonaForToday();

		if (userSessionCookie.isEmpty()) {
			HashedUserSessionId hashedUserSessionId = userSessionGenerator.generateNewHashedUserSessionId();
			EncodedHashedUserSessionId encodedHashedUserSessionId = userSessionTransformer.encodeHashedUserSessionId(hashedUserSessionId);

			log.info("Request with no session, giving {}", hashedUserSessionId.value());

			return controllerUtils.buildResponseWithUserSessionCookie(encodedHashedUserSessionId).body(
					ImmutableGetUserGuessesResponse.builder()
							.withGuesses(Collections.emptyList())
							.withTodayPersona(todayPersona)
							.build()
			);
		}

		HashedUserSessionId hashedUserSessionId = userSessionTransformer.decodeEncodedHashedUserSessionId(userSessionCookie.get());
		log.info("Request with session {}", hashedUserSessionId.value());

		Optional<DailyGuesses> todayGuesses = dailyGuessesRepository.getUserGuessesToday(hashedUserSessionId);
		List<String> personaGuesses = todayGuesses.map(DailyGuesses::guesses).orElse(Collections.emptyList());

		return ResponseEntity.ok(
				ImmutableGetUserGuessesResponse.builder()
						.withGuesses(personaGuesses)
						.withTodayPersona(todayPersona)
						.build()
		);
	}

	@PostMapping("/api/daily/guess")
	public ResponseEntity<Void> postUserGuess(
			HttpServletRequest rawRequest,
			@RequestBody PostUserGuessRequest body
	) {
		Map<String, String> cookies = controllerUtils.buildCookieMap(rawRequest);
		Optional<EncodedHashedUserSessionId> userSessionCookie = controllerUtils.getUserSessionCookie(cookies);

		HashedUserSessionId hashedUserSessionId = userSessionCookie.map(userSessionTransformer::decodeEncodedHashedUserSessionId)
				.orElse(userSessionGenerator.generateNewHashedUserSessionId());

		Optional<DailyGuesses> maybeDailyGuesses = dailyGuessesRepository.getUserGuessesToday(hashedUserSessionId);
		DailyGuesses updatedDailyGuesses = maybeDailyGuesses.map(
				existingGuesses -> ImmutableDailyGuesses.copyOf(existingGuesses).withGuesses(
						Stream.concat(existingGuesses.guesses().stream(), Stream.of(body.guess()))
								.distinct()
								.toList()
				)
		).orElse(
				ImmutableDailyGuesses.builder()
						.withHashedUserSessionId(hashedUserSessionId)
						.withDate(todaySupplier.get())
						.withGuesses(List.of(body.guess()))
						.build()
		);

		if (updatedDailyGuesses.guesses().size() > PersonleApiConstants.MAX_DAILY_GUESSES) {
			return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
		}

		dailyGuessesRepository.writeDailyGuesses(updatedDailyGuesses);

		if (userSessionCookie.isEmpty()) {
			log.info("Request with no session, giving {}", hashedUserSessionId.value());

			EncodedHashedUserSessionId encodedHashedUserSessionId = userSessionTransformer.encodeHashedUserSessionId(hashedUserSessionId);
			return controllerUtils.buildResponseWithUserSessionCookie(encodedHashedUserSessionId).build();
		}
		else {
			return ResponseEntity.ok(null);
		}
	}
}
