package guides;

import guides.extension.SystemOut;
import guides.extension.SystemOutCaptureExtension;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("Convert2MethodRef")
@ExtendWith(SystemOutCaptureExtension.class)
public class HandlingFailuresTest {

    @Test
    public void testInvoke(SystemOut out) {
        Uni<String> uni = Uni.createFrom().failure(new Exception("boom"));
        Multi<String> multi = Multi.createFrom().failure(new Exception("boom"));
        // tag::invoke[]
        Uni<String> u = uni
                .onFailure().invoke(failure -> log(failure));

        Multi<String> m = multi
                .onFailure().invoke(failure -> log(failure));
        // end::invoke[]
        assertThatThrownBy(() -> u.await().indefinitely()).hasMessageContaining("boom");
        assertThatThrownBy(() -> m.collectItems().asList()
                .await().indefinitely()).hasMessageContaining("boom");
        assertThat(out.get()).contains("boom");
    }

    @Test
    public void testTransform(SystemOut out) {
        Uni<String> uni = Uni.createFrom().failure(new Exception("boom"));
        // tag::transform[]
        Uni<String> u = uni
                .onFailure().transform(failure ->
                        new ServiceUnavailableException(failure));
        // end::transform[]
        assertThatThrownBy(() -> u.await().indefinitely())
                .hasCauseExactlyInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    public void testRecoverWithItem() {
        Uni<String> uni = Uni.createFrom().failure(new Exception("boom"));
        // tag::recover-item[]
        Uni<String> u1 = uni
                .onFailure().recoverWithItem("hello");

        Uni<String> u2 = uni
                .onFailure().recoverWithItem(f -> getFallback(f));
        // end::recover-item[]
        assertThat(u1.await().indefinitely()).isEqualTo("hello");
        assertThat(u2.await().indefinitely()).isEqualTo("hey");
    }

    @Test
    public void testCompletionOnFailure() {
        Multi<String> multi = Multi.createFrom().failure(new Exception("boom"));
        // tag::recover-completion[]
        Multi<String> m = multi
                .onFailure().recoverWithCompletion();
        // end::recover-completion[]
        assertThat(m.collectItems().asList().await().indefinitely()).isEmpty();
    }

    @Test
    public void testSwitch() {
        Uni<String> uni = Uni.createFrom().failure(new Exception("boom"));
        Multi<String> multi = Multi.createFrom().failure(new Exception("boom"));
        // tag::recover-switch[]
        Uni<String> u = uni
                .onFailure().recoverWithUni(f -> getFallbackUni(f));

        Multi<String> m = multi
                .onFailure().recoverWithMulti(f -> getFallbackMulti(f));
        // end::recover-switch[]
        assertThat(u.await().indefinitely()).isEqualTo("hello");
        assertThat(m.collectItems().asList().await().indefinitely()).containsExactly("hey");
    }

    private Multi<? extends String> getFallbackMulti(Throwable f) {
        return Multi.createFrom().item("hey");
    }

    private Uni<? extends String> getFallbackUni(Throwable f) {
        return Uni.createFrom().item("hello");
    }

    private String getFallback(Throwable f) {
        return "hey";
    }

    private void log(Throwable failure) {
        System.out.println(failure.getMessage());
    }

    private static class ServiceUnavailableException extends Throwable {
        public ServiceUnavailableException(Throwable ignored) {
        }
    }
}
