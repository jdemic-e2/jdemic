package jdemic.GameLogic;
//exista
import org.joml.Vector2f;

import java.awt.*;

public class Card {
    
    //Info related variables
    private final int id;
    private final String name;
    private final CardType cardType;
    private final Color cityColor;
    private String description;
    
    //Position related variables
    private Vector2f position;     // Base position
    private float currentYOffset;  // Animation offset
    private float targetYOffset;   // Target offset

    private final float MOVE_SPEED = 0.1f;
    private final float HOVER_DIST = 50.0f;

    private final float CARD_WIDTH = 0.05f;
    private final float CARD_HEIGHT = 0.18f;

    public Card(int id, String name, CardType type, Color color, float x, float y) {
        this.id = id;
        this.name = name;
        this.cardType = type;
        this.cityColor = color;
        this.position = new Vector2f(x, y);
        this.description = "";
    }
    
    public void setDescription(String description) {
        this.description = description;
    }

    //idk if this is how it should be but meh
    public void update(double mouseX, double mouseY) {
        boolean hovered = (mouseX >= position.x && mouseX <= position.x + CARD_WIDTH &&
                mouseY >= position.y + currentYOffset && mouseY <= position.y + CARD_HEIGHT + currentYOffset);

        if (hovered)
        {
            targetYOffset = -HOVER_DIST;
        }
        else
        {
            targetYOffset = 0.0f;
        }

        currentYOffset += (targetYOffset - currentYOffset) * MOVE_SPEED;
    }

    //Getters
    
    public int getId()
    {
        return id;
    }
    
    public String getName()
    {
        return name;
    }
    
    public CardType getCardType()
    {
        return cardType;
    }
    
    public Color getCityColor()
    {
        return cityColor;
    }
    
    public Vector2f getPosition()
    {
        return position;
    }

    public String getDescription() {
        return this.description;
    }
}