package com.example.data.db

object SeedData {
    val settings = listOf(
        SettingEntity(key = "OrgName", value = "ওয়াহীদিয়া ইসলামিয়া লাইব্রেরী"),
        SettingEntity(key = "Address", value = "রাণীবাজার, ঘোড়ামারা, বোয়ালিয়া, রাজশাহী"),
        SettingEntity(key = "GraceMinutes", value = "10"),
        SettingEntity(key = "WeeklyOff", value = "Fri"),
        SettingEntity(key = "MaxAccuracy", value = "100"),
        SettingEntity(key = "Timezone", value = "Asia/Dhaka"),
        SettingEntity(key = "FaceRequired", value = "0"),
        SettingEntity(key = "FaceThreshold", value = "0.55"),
        SettingEntity(key = "AdminPass", value = "admin123")
    )

    val branches = listOf(
        BranchEntity(branch = "প্রকাশনা", lat = "24.3629285", lng = "88.5968722", radius = "150"),
        BranchEntity(branch = "প্রেস", lat = "24.3629285", lng = "88.5968722", radius = "150"),
        BranchEntity(branch = "রাণীবাজার", lat = "24.366634", lng = "88.598017", radius = "150"),
        BranchEntity(branch = "সাহেববাজার", lat = "24.366634", lng = "88.598017", radius = "150"),
        BranchEntity(branch = "বাংলাবাজার", lat = "23.7057424551994", lng = "90.4121872181098", radius = "150")
    )

    val shifts = listOf(
        ShiftEntity(shift = "সাধারণ", startTime = "09:30", endTime = "21:30"),
        ShiftEntity(shift = "Exceptional", startTime = "10:00", endTime = "20:00")
    )

    val staff = listOf(
        StaffEntity(code = "01", name = "স্টাফ হুমায়ন কবির", designation = "Executive", branch = "প্রকাশনা", shift = "Exceptional", pin = "1234", status = "Active"),
        StaffEntity(code = "02", name = "মাকসুদ স্যার", designation = "গবেষক", branch = "প্রকাশনা", shift = "Exceptional", pin = "1234", status = "Active"),
        StaffEntity(code = "03", name = "(স্টাফ) ইয়াকুব আলী", designation = "মেশিনম্যান", branch = "প্রেস", shift = "সাধারণ", pin = "1234", status = "Active"),
        StaffEntity(code = "04", name = "(স্টাফ) রায়হান আলী", designation = "সেলসম্যান", branch = "বাংলাবাজার", shift = "সাধারণ", pin = "1234", status = "Active"),
        StaffEntity(code = "05", name = "(স্টাফ) জিয়াউল ইসলাম", designation = "হিসাবরক্ষক", branch = "প্রেস", shift = "সাধারণ", pin = "1234", status = "Active"),
        StaffEntity(code = "06", name = "মো. আলমগীর", designation = "সেলসম্যান", branch = "রাণীবাজার", shift = "সাধারণ", pin = "1234", status = "Active"),
        StaffEntity(code = "07", name = "মিজানুর (তেতুলিয়া)", designation = "সেলসম্যান", branch = "সাহেববাজার", shift = "সাধারণ", pin = "1234", status = "Active"),
        StaffEntity(code = "08", name = "মিজানুর রহমান (ভাতিজা)", designation = "সেলসম্যান", branch = "রাণীবাজার", shift = "সাধারণ", pin = "1234", status = "Active"),
        StaffEntity(code = "09", name = "(স্টাফ) আব্দুস সবুর", designation = "সেলসম্যান", branch = "সাহেববাজার", shift = "সাধারণ", pin = "1234", status = "Active"),
        StaffEntity(code = "10", name = "স্টাফ বেলাল হোসেন", designation = "সেলস ম্যানেজার", branch = "রাণীবাজার", shift = "সাধারণ", pin = "1234", status = "Active"),
        StaffEntity(code = "11", name = "সাব্বীর স্টাফ", designation = "সেলসম্যান", branch = "রাণীবাজার", shift = "সাধারণ", pin = "1234", status = "Active"),
        StaffEntity(code = "12", name = "(স্টাফ) ইসরাফিল", designation = "সেলসম্যান", branch = "সাহেববাজার", shift = "সাধারণ", pin = "1234", status = "Active"),
        StaffEntity(code = "13", name = "(স্টাফ) রনি ইসলাম", designation = "সেলস ম্যানেজার", branch = "বাংলাবাজার", shift = "সাধারণ", pin = "1234", status = "Active"),
        StaffEntity(code = "14", name = "রফিকুল ইসলাম", designation = "মেশিনম্যান", branch = "প্রেস", shift = "সাধারণ", pin = "1234", status = "Active"),
        StaffEntity(code = "15", name = "রাজিব", designation = "মেশিনম্যান", branch = "প্রেস", shift = "সাধারণ", pin = "1234", status = "Active"),
        StaffEntity(code = "16", name = "সোহাগ", designation = "সেলসম্যান", branch = "বাংলাবাজার", shift = "সাধারণ", pin = "1234", status = "Active")
    )

    val leaveTypes = listOf(
        LeaveTypeEntity(ltype = "সংরক্ষিত ছুটি", days = "12", paid = "Yes", kind = "Leave", probation = "6"),
        LeaveTypeEntity(ltype = "শাখা ছুটি", days = "", paid = "Yes", kind = "Holiday", probation = ""),
        LeaveTypeEntity(ltype = "বিনা বেতনে ছুটি", days = "", paid = "No", kind = "Leave", probation = "")
    )

    val holidays = listOf(
        HolidayEntity(dateFrom = "2026-12-16", dateTo = "2026-12-16", name = "বিজয় দিবস", branch = "")
    )

    val leaves = listOf(
        LeaveEntity(
            code = "13",
            dateFrom = "2026-08-05",
            dateTo = "2026-08-05",
            typ = "শাখা ছুটি",
            note = "গণ অভ্যুত্থান দিবস (পুরনো Holidays থেকে)",
            status = "Approved",
            appliedOn = "2026-08-05",
            reason = "গণ অভ্যুত্থান দিবস"
        ),
        LeaveEntity(
            code = "16",
            dateFrom = "2026-08-05",
            dateTo = "2026-08-05",
            typ = "শাখা ছুটি",
            note = "গণ অভ্যুত্থান দিবস (পুরনো Holidays থেকে)",
            status = "Approved",
            appliedOn = "2026-08-05",
            reason = "গণ অভ্যুত্থান দিবস"
        )
    )

    private fun att(
        d: String,
        c: String,
        n: String,
        b: String,
        inT: String,
        outT: String = "",
        inD: String = "",
        outD: String = "",
        src: String = "Imported"
    ) = AttendanceEntity(
        date = d,
        code = c,
        name = n,
        branch = b,
        inTime = inT,
        outTime = outT,
        inDist = inD,
        outDist = outD,
        source = src
    )

    // Historical records from setup.sql (recent months + sample history for reports/dashboard)
    val initialAttendance = listOf(
        att("2026-10-03", "09", "(স্টাফ) আব্দুস সবুর", "সাহেববাজার", "09:54", "21:39"),
        att("2026-10-03", "12", "(স্টাফ) ইসরাফিল", "সাহেববাজার", "10:01", "21:04"),
        att("2026-10-03", "14", "রফিকুল ইসলাম", "প্রেস", "09:41", ""),
        att("2026-10-03", "15", "রাজিব", "প্রেস", "09:41", "21:48"),
        att("2026-10-03", "16", "সোহাগ", "বাংলাবাজার", "09:49", "20:38"),
        att("2026-10-04", "09", "(স্টাফ) আব্দুস সবুর", "সাহেববাজার", "09:30", "21:55"),
        att("2026-10-04", "12", "(স্টাফ) ইসরাফিল", "সাহেববাজার", "09:47", "21:23"),
        att("2026-10-04", "14", "রফিকুল ইসলাম", "প্রেস", "09:53", ""),
        att("2026-10-04", "15", "রাজিব", "প্রেস", "10:00", "21:46"),
        att("2026-10-04", "16", "সোহাগ", "বাংলাবাজার", "10:10", "20:11"),
        att("2026-10-05", "09", "(স্টাফ) আব্দুস সবুর", "সাহেববাজার", "09:29", "20:10"),
        att("2026-10-05", "12", "(স্টাফ) ইসরাফিল", "সাহেববাজার", "09:47", "21:41"),
        att("2026-10-05", "13", "(স্টাফ) রনি ইসলাম", "বাংলাবাজার", "09:32", "20:29"),
        att("2026-10-05", "14", "রফিকুল ইসলাম", "প্রেস", "09:56", "21:41"),
        att("2026-10-05", "15", "রাজিব", "প্রেস", "09:59", "22:00"),
        att("2026-10-05", "16", "সোহাগ", "বাংলাবাজার", "10:12", "20:29"),
        att("2026-10-06", "09", "(স্টাফ) আব্দুস সবুর", "সাহেববাজার", "09:54", "21:53"),
        att("2026-10-06", "12", "(স্টাফ) ইসরাফিল", "সাহেববাজার", "09:45", "21:46"),
        att("2026-10-06", "13", "(স্টাফ) রনি ইসলাম", "বাংলাবাজার", "09:29", "20:40"),
        att("2026-10-06", "14", "রফিকুল ইসলাম", "প্রেস", "09:42", "21:36"),
        att("2026-10-06", "15", "রাজিব", "প্রেস", "11:46", "22:13"),
        att("2026-10-06", "16", "সোহাগ", "বাংলাবাজার", "09:58", "20:41"),
        att("2026-10-10", "12", "(স্টাফ) ইসরাফিল", "সাহেববাজার", "09:28", "21:43"),
        att("2026-10-10", "14", "রফিকুল ইসলাম", "প্রেস", "10:05", "21:46"),
        att("2026-10-10", "15", "রাজিব", "প্রেস", "10:12", "22:06"),
        att("2026-10-10", "16", "সোহাগ", "বাংলাবাজার", "10:12", "20:32"),
        att("2026-10-15", "09", "(স্টাফ) আব্দুস সবুর", "সাহেববাজার", "09:39", "21:24"),
        att("2026-10-15", "12", "(স্টাফ) ইসরাফিল", "সাহেববাজার", "10:25", ""),
        att("2026-10-15", "13", "(স্টাফ) রনি ইসলাম", "বাংলাবাজার", "10:19", ""),
        att("2026-10-15", "14", "রফিকুল ইসলাম", "প্রেস", "09:46", "22:13"),
        att("2026-10-15", "15", "রাজিব", "প্রেস", "10:25", "18:30"),
        att("2026-10-15", "16", "সোহাগ", "বাংলাবাজার", "10:23", ""),
        att("2026-10-20", "14", "রফিকুল ইসলাম", "প্রেস", "10:22", "21:51"),
        att("2026-10-20", "15", "রাজিব", "প্রেস", "10:12", "22:01"),
        att("2026-10-20", "16", "সোহাগ", "বাংলাবাজার", "10:13", "20:23"),
        att("2026-10-25", "09", "(স্টাফ) আব্দুস সবুর", "সাহেববাজার", "17:01", "21:47", "4", "1", "Mobile"),
        att("2026-10-26", "01", "স্টাফ হুমায়ন কবির", "প্রকাশনা", "10:00", "", "6442269", "", "Manual"),
        att("2026-10-26", "03", "(স্টাফ) ইয়াকুব আলী", "প্রেস", "12:30", "", "89", "", "Mobile"),
        att("2026-10-26", "04", "(স্টাফ) রায়হান আলী", "বাংলাবাজার", "10:31", "", "15", "", "Mobile"),
        att("2026-10-26", "05", "(স্টাফ) জিয়াউল ইসলাম", "প্রেস", "11:06", "", "9", "", "Mobile"),
        att("2026-10-26", "09", "(স্টাফ) আব্দুস সবুর", "সাহেববাজার", "09:38", "", "3", "", "Mobile"),
        att("2026-10-26", "12", "(স্টাফ) ইসরাফিল", "সাহেববাজার", "10:05", "", "383", "", "Mobile"),
        att("2026-10-26", "14", "রফিকুল ইসলাম", "প্রেস", "10:12", "", "8", "", "Mobile"),
        att("2026-10-26", "15", "রাজিব", "প্রেস", "10:13", "", "12", "", "Mobile"),
        // November 2026 recent baseline entries so current-month dashboard (2026-11) is immediately populated
        att("2026-11-01", "01", "স্টাফ হুমায়ন কবির", "প্রকাশনা", "09:58", "20:05", "12", "14", "Mobile"),
        att("2026-11-01", "03", "(স্টাফ) ইয়াকুব আলী", "প্রেস", "09:28", "21:32", "20", "18", "Mobile"),
        att("2026-11-01", "04", "(স্টাফ) রায়হান আলী", "বাংলাবাজার", "09:45", "21:20", "15", "12", "Mobile"),
        att("2026-11-01", "05", "(স্টাফ) জিয়াউল ইসলাম", "প্রেস", "09:34", "21:35", "9", "10", "Mobile"),
        att("2026-11-01", "09", "(স্টাফ) আব্দুস সবুর", "সাহেববাজার", "09:36", "21:40", "5", "4", "Mobile"),
        att("2026-11-02", "01", "স্টাফ হুমায়ন কবির", "প্রকাশনা", "10:04", "20:10", "11", "10", "Mobile"),
        att("2026-11-02", "04", "(স্টাফ) রায়হান আলী", "বাংলাবাজার", "09:35", "21:25", "18", "14", "Mobile"),
        att("2026-11-02", "09", "(স্টাফ) আব্দুস সবুর", "সাহেববাজার", "09:48", "21:45", "7", "6", "Mobile"),
        att("2026-11-02", "14", "রফিকুল ইসলাম", "প্রেস", "09:31", "21:42", "8", "10", "Mobile"),
        att("2026-11-02", "15", "রাজিব", "প্রেস", "09:52", "21:55", "12", "11", "Mobile")
    )
}
