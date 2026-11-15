package com.pronotes.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

class ProNotesIssueRegistry : IssueRegistry() {
    override val issues: List<Issue> = listOf(
        DirectGlassUsageDetector.ISSUE,
    )

    override val api: Int = CURRENT_API
    override val minApi: Int = 14

    override val vendor: Vendor = Vendor(
        vendorName = "ProNotes",
        identifier = "com.pronotes.lint",
    )
}
