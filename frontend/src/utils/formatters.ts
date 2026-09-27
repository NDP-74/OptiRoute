export const formatNumber = (value: number | null, suffix = "", maximumFractionDigits = 2): string => {
    if (value === null) {
        return "Non renseigné"
    }

    return `${value.toLocaleString("fr-FR", { maximumFractionDigits })}${suffix}`
}

export const formatDistance = (value: number | null): string => {
    if (value === null) {
        return "Non renseignée";
    }

    return `${(value / 1000).toLocaleString("fr-FR", {
        minimumFractionDigits: 0,
        maximumFractionDigits: 1,
    })} km`;
}

export const formatCurrency = (value: number | null): string => {
    if (value === null) {
        return "Non renseigné"
    }

    return value.toLocaleString("fr-FR", {
        style: "currency",
        currency: "EUR",
        maximumFractionDigits: 2,
    })
}

export const formatDate = (value: string | null,): string => {
    if (!value) {
        return "Non renseignée"
    }

    const [year, month, day] = value.split("-").map(Number)

    if (!year || !month || !day) {
        return value
    }

    return new Intl.DateTimeFormat("fr-FR").format(
        new Date(year, month - 1, day),
    )
}

export const formatDateTime = (value: string | null | undefined,): string => {
    if (!value) {
        return "Non renseignée"
    }

    return new Intl.DateTimeFormat("fr-FR", {
        dateStyle: "medium",
        timeStyle: "short",
    }).format(new Date(value))
}

export const formatParisDateTime = (value: string | null | undefined): string => {
    if (!value) {
        return "Non renseignée"
    }

    const date = new Date(value)

    if (Number.isNaN(date.getTime())) {
        return value
    }

    return new Intl.DateTimeFormat("fr-FR", {
        timeZone: "Europe/Paris",
        dateStyle: "medium",
        timeStyle: "short",
    }).format(date)
}

export const formatParisTime = (value: string): string => {
    const date = new Date(value)

    if (Number.isNaN(date.getTime())) {
        return "--:--"
    }

    return new Intl.DateTimeFormat("fr-FR", {
        timeZone: "Europe/Paris",
        hour: "2-digit",
        minute: "2-digit",
    }).format(date)
}

export const formatParisDateTimeLocal = (value: string): string => {
    const parts = new Intl.DateTimeFormat("en", {
        timeZone: "Europe/Paris",
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
        hourCycle: "h23",
    }).formatToParts(new Date(value))
    const part = (type: string) => parts.find(item => item.type === type)?.value ?? ""

    return `${part("year")}-${part("month")}-${part("day")}T${part("hour")}:${part("minute")}`
}

export const parisDateTimeLocalToIso = (value: string): string => {
    const [datePart, timePart] = value.split("T")
    const [year = 0, month = 0, day = 0] = (datePart ?? "").split("-").map(Number)
    const [hour = 0, minute = 0] = (timePart ?? "").split(":").map(Number)
    const targetUtc = Date.UTC(year, month - 1, day, hour, minute)
    let instant = targetUtc

    for (let attempt = 0; attempt < 3; attempt++) {
        const parisParts = new Intl.DateTimeFormat("en", {
            timeZone: "Europe/Paris",
            year: "numeric",
            month: "2-digit",
            day: "2-digit",
            hour: "2-digit",
            minute: "2-digit",
            hourCycle: "h23",
        }).formatToParts(new Date(instant))
        const part = (type: string) => Number(parisParts.find(item => item.type === type)?.value ?? 0)
        const parisAsUtc = Date.UTC(part("year"), part("month") - 1, part("day"), part("hour"), part("minute"))

        instant += targetUtc - parisAsUtc
    }

    return new Date(instant).toISOString()
}

export const formatParisDateKey = (value: string): string => {
    const parts = new Intl.DateTimeFormat("en", {
        timeZone: "Europe/Paris",
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
    }).formatToParts(new Date(value))
    const part = (type: string) => parts.find(item => item.type === type)?.value ?? ""

    return `${part("year")}-${part("month")}-${part("day")}`
}

export const formatDurationSeconds = (value: number | null): string => {
    if (value === null) {
        return "Non renseignée";
    }


    if (value <= 60) {
        return "1 min";
    }

    const hours = Math.floor(value / 3600);
    const minutes = Math.round((value % 3600) / 60);

    if (hours === 0) {
        return `${minutes} mins`;
    }

    if (minutes === 0) {
        return `${hours} h`;
    }

    return `${hours} h ${String(minutes).padStart(2, "0")}`;
}

export const formatDurationMinutes = (value: number | null): string => {
    if (value === null) {
        return "Non renseignée";
    }

    const hours = Math.floor(value / 60);
    const minutes = value % 60;

    if (hours === 0) {
        return `${minutes} min${minutes > 1 ? "s" : ""}`;
    }

    if (minutes === 0) {
        return `${hours} h`;
    }

    return `${hours} h ${String(minutes).padStart(2, "0")} min`;
};