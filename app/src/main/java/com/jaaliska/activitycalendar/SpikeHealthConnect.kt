package com.jaaliska.activitycalendar

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.temporal.ChronoUnit

const val SPIKE_TAG = "SpikeHC"

val SPIKE_PERMISSIONS = setOf(
    HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    HealthPermission.getReadPermission(DistanceRecord::class),
    HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY,
)

/**
 * Числовые константы EXERCISE_TYPE_* достаются рефлексией, а не переписываются руками:
 * их около восьмидесяти, и любая опечатка в ручной таблице испортит выводы спайка.
 */
private val exerciseTypeNames: Map<Int, String> by lazy {
    ExerciseSessionRecord::class.java.declaredFields
        .filter { it.name.startsWith("EXERCISE_TYPE_") && it.type == Int::class.javaPrimitiveType }
        .mapNotNull { field ->
            field.isAccessible = true
            (field.get(null) as? Int)?.let { it to field.name }
        }
        .toMap()
}

fun typeName(type: Int): String = exerciseTypeNames[type] ?: "UNKNOWN"

suspend fun runSpike(context: Context, daysBack: Long): List<String> {
    val out = mutableListOf<String>()
    fun emit(line: String) {
        Log.i(SPIKE_TAG, line)
        out += line
    }

    when (val status = HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> Unit
        else -> {
            emit("Health Connect недоступен, sdkStatus=$status")
            return out
        }
    }

    val client = HealthConnectClient.getOrCreate(context)

    val granted = client.permissionController.getGrantedPermissions()
    emit("Выдано разрешений: ${granted.size}")
    SPIKE_PERMISSIONS.forEach { emit("  ${if (it in granted) "+" else "-"} $it") }
    if (!granted.containsAll(SPIKE_PERMISSIONS)) {
        emit("Не все разрешения выданы — читаем что дают, но глубина может быть урезана")
    }

    val end = Instant.now()
    val start = end.minus(daysBack, ChronoUnit.DAYS)
    emit("")
    emit("=== Читаем ExerciseSessionRecord за $daysBack дней: $start .. $end ===")

    val sessions = mutableListOf<ExerciseSessionRecord>()
    var pageToken: String? = null
    do {
        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                pageToken = pageToken,
            )
        )
        sessions += response.records
        pageToken = response.pageToken
    } while (pageToken != null)

    emit("Всего сессий: ${sessions.size}")
    if (sessions.isEmpty()) return out

    emit("Самая ранняя: ${sessions.minOf { it.startTime }}")
    emit("Самая поздняя: ${sessions.maxOf { it.startTime }}")

    emit("")
    emit("=== Типы активностей ===")
    sessions.groupingBy { it.exerciseType }.eachCount()
        .toList().sortedByDescending { it.second }
        .forEach { (type, count) -> emit("  $count x ${typeName(type)} ($type)") }

    emit("")
    emit("=== Источники данных ===")
    sessions.groupingBy { it.metadata.dataOrigin.packageName }.eachCount()
        .forEach { (pkg, count) -> emit("  $count x $pkg") }

    emit("")
    emit("=== Заполненность полей по всем сессиям ===")
    emit("  startZoneOffset заполнен: ${sessions.count { it.startZoneOffset != null }} / ${sessions.size}")
    emit("  title заполнен:           ${sessions.count { !it.title.isNullOrBlank() }} / ${sessions.size}")
    emit("  notes заполнены:          ${sessions.count { !it.notes.isNullOrBlank() }} / ${sessions.size}")
    emit("  clientRecordId заполнен:  ${sessions.count { it.metadata.clientRecordId != null }} / ${sessions.size}")
    emit("  есть segments:            ${sessions.count { it.segments.isNotEmpty() }} / ${sessions.size}")
    emit("  есть laps:                ${sessions.count { it.laps.isNotEmpty() }} / ${sessions.size}")

    emit("")
    emit("=== Подробно по последним сессиям ===")
    sessions.sortedByDescending { it.startTime }.take(10).forEach { s ->
        emit("")
        emit("startTime      = ${s.startTime}")
        emit("startZoneOffset= ${s.startZoneOffset}")
        emit("endTime        = ${s.endTime}")
        emit("endZoneOffset  = ${s.endZoneOffset}")
        emit("exerciseType   = ${s.exerciseType} (${typeName(s.exerciseType)})")
        emit("title          = ${s.title}")
        emit("notes          = ${s.notes}")
        emit("metadata.id    = ${s.metadata.id}")
        emit("clientRecordId = ${s.metadata.clientRecordId}")
        emit("dataOrigin     = ${s.metadata.dataOrigin.packageName}")
        emit("lastModified   = ${s.metadata.lastModifiedTime}")
        emit("segments/laps  = ${s.segments.size} / ${s.laps.size}")

        // Дистанции в ExerciseSessionRecord нет - она отдельной записью.
        // dataOriginFilter, чтобы не приплюсовать метры от другого приложения.
        val origin = setOf(s.metadata.dataOrigin)
        val window = TimeRangeFilter.between(s.startTime, s.endTime)
        val meters = runCatching {
            client.aggregate(
                AggregateRequest(
                    metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                    timeRangeFilter = window,
                    dataOriginFilter = origin,
                )
            )[DistanceRecord.DISTANCE_TOTAL]?.inMeters
        }.getOrElse { "ошибка: ${it.message}" }
        emit("distance(агрег.) = $meters м")

        val rawDistance = runCatching {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = DistanceRecord::class,
                    timeRangeFilter = window,
                    dataOriginFilter = origin,
                )
            ).records
        }.getOrElse { emptyList() }
        emit("DistanceRecord   = ${rawDistance.size} шт." +
            if (rawDistance.isNotEmpty()) ", первая ${rawDistance.first().distance.inMeters} м" else "")
    }

    return out
}
