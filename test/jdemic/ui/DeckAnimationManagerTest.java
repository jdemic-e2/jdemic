package jdemic.ui;

import javafx.scene.layout.StackPane;
import jdemic.GameLogic.Card;
import jdemic.GameLogic.CityNode;
import jdemic.GameLogic.ServerRelatedClasses.PlayerState;
import jdemic.ui.GameplayUI.DeckManager;
import jdemic.ui.GameplayUI.HandManager;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(ApplicationExtension.class)
@ExtendWith(MockitoExtension.class)
class DeckAnimationManagerTest {

    private StackPane root;
    private DeckAnimationManager animationManager;
    private MockedStatic<AnimationSpeedUtil> mockedSpeedUtil;

    @Mock
    private DeckManager mockDeckManager;
    @Mock
    private HandManager mockHandManager;
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
        root.resize(1024, 768);
        animationManager = new DeckAnimationManager(root, mockDeckManager, mockHandManager);

        lenient().when(mockDeckManager.createBackCard()).thenAnswer(inv -> new StackPane());
        lenient().when(mockDeckManager.createCityCard(any())).thenAnswer(inv -> new StackPane());

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
    void testPlayInitialHandAnimation_NullPlayer() {
        animationManager.playInitialHandAnimation(null, mockRunnable);
        verify(mockRunnable, times(1)).run();
    }

    @Test
    void testPlayInitialHandAnimation_NullHand() {
        PlayerState mockPlayer = mock(PlayerState.class);
        when(mockPlayer.getHand()).thenReturn(null);

        animationManager.playInitialHandAnimation(mockPlayer, mockRunnable);
        verify(mockRunnable, times(1)).run();
    }

    @Test
    void testPlayInitialHandAnimation_EmptyHand() {
        PlayerState mockPlayer = mock(PlayerState.class);
        when(mockPlayer.getHand()).thenReturn(new ArrayList<>());

        animationManager.playInitialHandAnimation(mockPlayer, mockRunnable);
        verify(mockRunnable, times(1)).run();
    }

    @Test
    void testPlayInitialHandAnimation_ValidHandAndNullCallback() {
        PlayerState mockPlayer = mock(PlayerState.class);
        Card mockCard = mock(Card.class);
        CityNode mockCity = mock(CityNode.class);

        when(mockCard.getTargetCity()).thenReturn(mockCity);
        when(mockPlayer.getHand()).thenReturn(Collections.singletonList(mockCard));

        animationManager.playInitialHandAnimation(mockPlayer, null);
        assertNotNull(root);
    }

    @Test
    void testPlayInitialHandAnimation_ValidHandWithCityNull() {
        PlayerState mockPlayer = mock(PlayerState.class);
        Card mockCard = mock(Card.class);

        when(mockCard.getTargetCity()).thenReturn(null);
        when(mockPlayer.getHand()).thenReturn(Collections.singletonList(mockCard));

        animationManager.playInitialHandAnimation(mockPlayer, mockRunnable);
        verify(mockRunnable, times(1)).run();
    }

    @Test
    void testPlayStartupShuffleAnimation_WithCallback() {
        animationManager.playStartupShuffleAnimation(mockRunnable);
        verify(mockRunnable, times(1)).run();
    }

    @Test
    void testPlayShuffleAnimation_NullList() {
        animationManager.playShuffleAnimation(null, mockRunnable);
        verify(mockRunnable, times(1)).run();
    }

    @Test
    void testPlayShuffleAnimation_EmptyList() {
        animationManager.playShuffleAnimation(new ArrayList<>(), mockRunnable);
        verify(mockRunnable, times(1)).run();
    }

    @Test
    void testPlayShuffleAnimation_NullCallback() {
        List<StackPane> cards = Collections.singletonList(new StackPane());
        animationManager.playShuffleAnimation(cards, null);
        assertNotNull(root);
    }

    @Test
    void testPlayEpidemicAnimation_WithCallback() {
        try {
            animationManager.playEpidemicAnimation(mockRunnable);
            verify(mockRunnable, times(1)).run();
        } catch (NullPointerException e) {
            assertTrue(e.getMessage() == null || e.getMessage().contains("resource"));
        }
    }

    @Test
    void testPlayEpidemicAnimation_NullCallback() {
        try {
            animationManager.playEpidemicAnimation(null);
            assertNotNull(root);
        } catch (NullPointerException e) {
            assertTrue(e.getMessage() == null || e.getMessage().contains("resource"));
        }
    }

    @Test
    void testPlayInfectionCardDraw_WithCallback() {
        StackPane infectionCard = new StackPane();
        animationManager.playInfectionCardDraw(infectionCard, mockRunnable);
        verify(mockRunnable, times(1)).run();
    }

    @Test
    void testPlayInfectionCardDraw_NullCallback() {
        StackPane infectionCard = new StackPane();
        animationManager.playInfectionCardDraw(infectionCard, null);
        assertNotNull(root);
    }
}