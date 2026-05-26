package jdemic.ui;

import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import jdemic.GameLogic.DiseaseColor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testfx.framework.junit5.ApplicationExtension;
import javafx.animation.Animation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(ApplicationExtension.class)
@ExtendWith(MockitoExtension.class)
class TurnAnimationManagerTest {

    private StackPane root;
    private TurnAnimationManager animationManager;
    private MockedStatic<AnimationSpeedUtil> mockedSpeedUtil;

    @Mock
    private Runnable mockRunnable;

    @BeforeAll
    static void setupHeadlessMode() {
        System.setProperty("glass.platform", "Monocle");
        System.setProperty("monocle.platform", "Headless");
        System.setProperty("prism.order", "sw");
    }

    @BeforeEach
    void setUp() {
        root = new StackPane();
        animationManager = new TurnAnimationManager(root);

        // Intercept AnimationSpeedUtil.play and force instant callback execution
        mockedSpeedUtil = Mockito.mockStatic(AnimationSpeedUtil.class);
        mockedSpeedUtil.when(() -> AnimationSpeedUtil.play(any(Animation.class)))
                .thenAnswer(invocation -> {
                    Animation anim = invocation.getArgument(0);
                    if (anim.getOnFinished() != null) {
                        anim.getOnFinished().handle(null);
                    }
                    return null;
                });
    }

    @AfterEach
    void tearDown() {
        if (mockedSpeedUtil != null) {
            mockedSpeedUtil.close();
        }
    }

    @Test
    void testPlayTurnStart_WithCallback() {
        animationManager.playTurnStart("Player1", mockRunnable);
        verify(mockRunnable, times(1)).run();
    }

    @Test
    void testPlayTurnStart_NullCallback() {
        animationManager.playTurnStart("Player1", null);
        assertNotNull(root);
    }

    @Test
    void testPlayTurnEnd_WithCallback() {
        animationManager.playTurnEnd(mockRunnable);
        verify(mockRunnable, times(1)).run();
    }

    @Test
    void testPlayTurnEnd_NullCallback() {
        animationManager.playTurnEnd(null);
        assertNotNull(root);
    }

    @Test
    void testPlayActionConsumed_WithCallback() {
        animationManager.playActionConsumed(mockRunnable);
        verify(mockRunnable, times(1)).run();
    }

    @Test
    void testPlayActionConsumed_NullCallback() {
        animationManager.playActionConsumed(null);
        assertNotNull(root);
    }

    @Test
    void testPlayInfectionPhaseIntro_WithCallback() {
        animationManager.playInfectionPhaseIntro(mockRunnable);
        verify(mockRunnable, times(1)).run();
    }

    @Test
    void testPlayInfectionPhaseIntro_NullCallback() {
        animationManager.playInfectionPhaseIntro(null);
        assertNotNull(root);
    }

    @Test
    void testPlayCityInfection_WithCallback() {
        Node spyNode = Mockito.spy(new Circle());
        animationManager.playCityInfection(spyNode, mockRunnable);
        verify(mockRunnable, times(1)).run();
    }

    @Test
    void testPlayCityInfection_NullCallback() {
        Node spyNode = Mockito.spy(new Circle());
        animationManager.playCityInfection(spyNode, null);
        assertNotNull(root);
    }

    @Test
    void testPlayDiseaseTreated_WithCallback() {
        Node spyNode = Mockito.spy(new Circle());
        animationManager.playDiseaseTreated(spyNode, mockRunnable);
        verify(mockRunnable, times(1)).run();
    }

    @Test
    void testPlayDiseaseTreated_NullCallback() {
        Node spyNode = Mockito.spy(new Circle());
        animationManager.playDiseaseTreated(spyNode, null);
        assertNotNull(root);
    }

    @Test
    void testPlayOutbreak_WithCallback() {
        Node spyNode = Mockito.spy(new Circle());
        animationManager.playOutbreak(spyNode, mockRunnable);
        verify(mockRunnable, times(1)).run();
    }

    @Test
    void testPlayOutbreak_NullCallback() {
        Node spyNode = Mockito.spy(new Circle());
        animationManager.playOutbreak(spyNode, null);
        assertNotNull(root);
    }

    @Test
    void testPlayCureDiscovered_Blue_WithCallback() {
        animationManager.playCureDiscovered(DiseaseColor.BLUE, mockRunnable);
        verify(mockRunnable, times(1)).run();
    }

    @Test
    void testPlayCureDiscovered_Yellow() {
        animationManager.playCureDiscovered(DiseaseColor.YELLOW, null);
        assertNotNull(root);
    }

    @Test
    void testPlayCureDiscovered_Black() {
        animationManager.playCureDiscovered(DiseaseColor.BLACK, null);
        assertNotNull(root);
    }

    @Test
    void testPlayCureDiscovered_Red() {
        animationManager.playCureDiscovered(DiseaseColor.RED, null);
        assertNotNull(root);
    }

    @Test
    void testPlayVictory_WithCallback() {
        animationManager.playVictory(mockRunnable);
        verify(mockRunnable, times(1)).run();
    }

    @Test
    void testPlayVictory_NullCallback() {
        animationManager.playVictory(null);
        assertNotNull(root);
    }

    @Test
    void testPlayDefeat_WithCallback() {
        animationManager.playDefeat(mockRunnable);
        verify(mockRunnable, times(1)).run();
    }

    @Test
    void testPlayDefeat_NullCallback() {
        animationManager.playDefeat(null);
        assertNotNull(root);
    }

    @Test
    void testPlayResearchStationBuilt_WithCallback() {
        Node spyNode = Mockito.spy(new Circle());
        animationManager.playResearchStationBuilt(spyNode, mockRunnable);
        verify(mockRunnable, times(1)).run();
    }

    @Test
    void testPlayResearchStationBuilt_NullCallback() {
        Node spyNode = Mockito.spy(new Circle());
        animationManager.playResearchStationBuilt(spyNode, null);
        assertNotNull(root);
    }
}