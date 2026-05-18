package code._02_Abstraction;

/**
 * Demonstrates ABSTRACTION via interface + abstract class.
 *
 * Pattern shown: Interface (contract) → Abstract class (shared impl) → Concrete class
 * This is exactly how Java's List → AbstractList → ArrayList works.
 *
 * Interview tip: Define the interface (contract) first. Worry about implementation later.
 */

// -------------------------------------------------------------------------
// STEP 1: Interface — defines WHAT, hides HOW
// -------------------------------------------------------------------------
interface Shape {
    double area();
    double perimeter();
    void draw();          // capability contract — every shape can draw itself
}

// -------------------------------------------------------------------------
// STEP 2: Abstract class — shared behaviour that all shapes reuse
// -------------------------------------------------------------------------
abstract class AbstractShape implements Shape {

    private final String color;

    protected AbstractShape(String color) {
        this.color = color;
    }

    // Shared implementation — all shapes draw with their color
    @Override
    public void draw() {
        System.out.printf("Drawing %s [color=%s, area=%.2f]%n",
                getClass().getSimpleName(), color, area());
    }

    // Shared utility — concrete classes get this for free
    public String describe() {
        return String.format("%s: area=%.2f, perimeter=%.2f", getClass().getSimpleName(), area(), perimeter());
    }

    public String getColor() { return color; }
}

// -------------------------------------------------------------------------
// STEP 3: Concrete classes — implement HOW
// -------------------------------------------------------------------------
class Circle extends AbstractShape {
    private final double radius;

    public Circle(double radius, String color) {
        super(color);
        this.radius = radius;
    }

    @Override
    public double area() { return Math.PI * radius * radius; }

    @Override
    public double perimeter() { return 2 * Math.PI * radius; }
}

class Rectangle extends AbstractShape {
    private final double width;
    private final double height;

    public Rectangle(double width, double height, String color) {
        super(color);
        this.width = width;
        this.height = height;
    }

    @Override
    public double area() { return width * height; }

    @Override
    public double perimeter() { return 2 * (width + height); }
}

class Triangle extends AbstractShape {
    private final double a, b, c;

    public Triangle(double a, double b, double c, String color) {
        super(color);
        this.a = a; this.b = b; this.c = c;
    }

    @Override
    public double area() {
        double s = (a + b + c) / 2;
        return Math.sqrt(s * (s - a) * (s - b) * (s - c));
    }

    @Override
    public double perimeter() { return a + b + c; }
}

// -------------------------------------------------------------------------
// CALLER CODE — works with Shape interface, unaware of concrete types
// -------------------------------------------------------------------------
class ShapeDemo {
    public static void main(String[] args) {
        // Caller uses the abstraction (Shape), not the concrete type
        Shape[] shapes = {
            new Circle(5, "red"),
            new Rectangle(4, 6, "blue"),
            new Triangle(3, 4, 5, "green")
        };

        double totalArea = 0;
        for (Shape shape : shapes) {
            shape.draw();
            totalArea += shape.area();
        }

        System.out.printf("Total area of all shapes: %.2f%n", totalArea);
        // If we add a Pentagon class tomorrow, this loop needs ZERO changes — abstraction wins
    }
}
