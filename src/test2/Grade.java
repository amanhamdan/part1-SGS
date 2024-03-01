package test2;
public class Grade {
    private int gradeId;
    private User student;
    private Course course;
    private float grade;

    public Grade(int gradeId, User student, Course course, float grade) {
        this.gradeId = gradeId;
        this.student = student;
        this.course = course;
        this.grade = grade;
    }

    public int getGradeId() {
        return gradeId;
    }

    public void setGradeId(int gradeId) {
        this.gradeId = gradeId;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public float getGrade() {
        return grade;
    }

    public void setGrade(float grade) {
        this.grade = grade;
    }
}