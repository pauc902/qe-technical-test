package com.example.qe.reporting;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * After a test run, writes one readable table to target/test-summary.md:
 * Result | Layer | Area | Type | Test.
 * Layer comes from the tags api/ui, Type from positive/negative/known-defect, and Area from @Area.
 * JUnit finds this class through META-INF/services, so it runs on every `mvn test` with no set-up.
 */
public class SummaryReportListener implements TestExecutionListener {

    private static final Path OUTPUT = Paths.get("target", "test-summary.md");

    private record Row(int order, String result, String layer, String area, String type, String test) {
    }

    private final List<Row> rows = new ArrayList<>();
    private TestPlan plan;

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        this.plan = testPlan;
    }

    @Override
    public synchronized void executionSkipped(TestIdentifier id, String reason) {
        // A @Disabled parameterised test is skipped as a whole, so accept the method-level entry.
        if (id.getSource().filter(MethodSource.class::isInstance).isPresent()) {
            add(id, "⏭️ Skipped");
        }
    }

    @Override
    public synchronized void executionFinished(TestIdentifier id, TestExecutionResult result) {
        if (!id.isTest()) {
            return;
        }
        add(id, switch (result.getStatus()) {
            case SUCCESSFUL -> "✅ Passed";
            case FAILED -> "❌ Failed";
            case ABORTED -> "⚠️ Aborted";
        });
    }

    @Override
    public synchronized void testPlanExecutionFinished(TestPlan testPlan) {
        if (rows.isEmpty()) {
            return;
        }
        rows.sort(Comparator.comparingInt((Row r) -> r.layer().equals("API") ? 0 : 1)
                .thenComparingInt(r -> r.type().startsWith("Positive") ? 0 : r.type().contains("known defect") ? 2 : 1)
                .thenComparingInt(Row::order));
        long passed = rows.stream().filter(r -> r.result().contains("Passed")).count();
        long failed = rows.stream().filter(r -> r.result().contains("Failed")).count();
        long skipped = rows.stream().filter(r -> r.result().contains("Skipped")).count();

        StringBuilder md = new StringBuilder()
                .append("## Test results\n\n")
                .append("**").append(passed).append(" passed**, ")
                .append(failed).append(" failed, ")
                .append(skipped).append(" skipped (").append(rows.size()).append(" total)\n\n")
                .append("| Result | Layer | Area | Type | Test |\n")
                .append("|---|---|---|---|---|\n");
        rows.forEach(r -> md.append("| ").append(r.result())
                .append(" | ").append(r.layer())
                .append(" | ").append(r.area())
                .append(" | ").append(r.type())
                .append(" | ").append(r.test().replace("|", "\\|"))
                .append(" |\n"));
        try {
            Files.createDirectories(OUTPUT.getParent());
            Files.writeString(OUTPUT, md.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void add(TestIdentifier id, String result) {
        Set<String> tags = allTags(id);
        String layer = tags.contains("api") ? "API" : tags.contains("ui") ? "UI" : "-";
        String type = tags.contains("known-defect") ? "Negative (known defect)"
                : tags.contains("positive") ? "Positive"
                : tags.contains("negative") ? "Negative" : "-";
        rows.add(new Row(rows.size(), result, layer, area(id.getSource().orElse(null)), type, name(id)));
    }

    /** The tags on this test plus those on everything that contains it (method, class). */
    private Set<String> allTags(TestIdentifier id) {
        Set<String> tags = new HashSet<>();
        for (TestIdentifier cur = id; cur != null; cur = plan.getParent(cur).orElse(null)) {
            cur.getTags().stream().map(TestTag::getName).forEach(tags::add);
        }
        return tags;
    }

    /** A parameterised case is shown as "<test name> - <case name>". */
    private String name(TestIdentifier id) {
        String own = id.getDisplayName();
        return plan.getParent(id)
                .filter(p -> p.getSource().filter(MethodSource.class::isInstance).isPresent())
                .map(p -> p.getDisplayName() + " - " + own.replaceFirst("^\\[\\d+]\\s*", ""))
                .orElse(own);
    }

    private static String area(TestSource source) {
        if (!(source instanceof MethodSource ms)) {
            return "-";
        }
        try {
            if (ms.getJavaMethod().isAnnotationPresent(Area.class)) {
                return ms.getJavaMethod().getAnnotation(Area.class).value();
            }
            Class<?> c = ms.getJavaClass();
            if (c.isAnnotationPresent(Area.class)) {
                return c.getAnnotation(Area.class).value();
            }
        } catch (RuntimeException ignored) {
            // A reporting problem must never fail the test run.
        }
        return "-";
    }
}
