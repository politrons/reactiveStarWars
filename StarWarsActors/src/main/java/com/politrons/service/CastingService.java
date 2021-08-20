package com.politrons.service;

import io.vavr.concurrent.Future;

import java.util.Map;


public class CastingService {

    Map<String, String> actors = Map.of(
            "episode1", "Anakin, Owi-wan, Qui-Gon-Jin",
            "episode2", "Anakin, Owi-wan, Mace-Windu",
            "episode3", "Anakin, Owi-wan, Palpatine");

    public Future<String> getCastingForEpisode(String episode) {
        return Future.of(() -> actors.getOrDefault(episode, "No info"));
    }
}

