package se.carebridge.patient;

public class Patient {
    private Long id;
    private String firstName;
    private String lastName;
    private int birthYear;

    public Patient(Long id, String firstName, String lastName, int birthYear) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthYear = birthYear;
    }

    public Long getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public int getBirthYear() { return birthYear; }
}
