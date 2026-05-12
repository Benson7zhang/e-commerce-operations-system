package com.emall.channel.adapter.model;

import java.util.List;

public record PageResult<T>(List<T> data, int totalPages, boolean hasMore) {
}
