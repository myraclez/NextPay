package me.myraclez.nextPayAPI;

import java.util.UUID;

public record PlayerSettings(UUID uuid, boolean payments, boolean notifications) {
}
