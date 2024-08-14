package net.bnbdiscord.borderkit.database;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

public class Jurisdiction {
    @DatabaseField(id = true)
    private String code;

    @DatabaseField
    private String name;

    @DatabaseField
    private String department;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    @DatabaseField(defaultValue = "BIOMETRIC PAGE")
    private String biometricPageString;

    public String getBiometricPageString() {
        return biometricPageString;
    }

    public void setBiometricPageString(String biometricPageString) {
        this.biometricPageString = biometricPageString;
    }

    @DatabaseField(defaultValue = "NAMES")
    private String namesString;

    public String getNamesString() {
        return namesString;
    }

    public void setNamesString(String namesString) {
        this.namesString = namesString;
    }

    @DatabaseField(defaultValue = "FAMILY")
    private String familyString;

    public String getFamilyString() {
        return familyString;
    }

    public void setFamilyString(String familyString) {
        this.familyString = familyString;
    }

    @DatabaseField(defaultValue = "GIVEN")
    private String givenString;

    public String getGivenString() {
        return givenString;
    }

    public void setGivenString(String givenString) {
        this.givenString = givenString;
    }

    @DatabaseField(defaultValue = "DOCUMENT NUMBER")
    private String documentNumberString;

    public String getDocumentNumberString() {
        return documentNumberString;
    }

    public void setDocumentNumberString(String documentNumberString) {
        this.documentNumberString = documentNumberString;
    }

    @DatabaseField(defaultValue = "PLACE OF BIRTH")
    private String placeOfBirthString;

    public String getPlaceOfBirthString() {
        return placeOfBirthString;
    }

    public void setPlaceOfBirthString(String placeOfBirthString) {
        this.placeOfBirthString = placeOfBirthString;
    }

    @DatabaseField(defaultValue = "DATE OF BIRTH")
    private String dateOfBirthString;

    public String getDateOfBirthString() {
        return dateOfBirthString;
    }

    public void setDateOfBirthString(String dateOfBirthString) {
        this.dateOfBirthString = dateOfBirthString;
    }

    @DatabaseField(defaultValue = "AUTHORITY / CODE")
    private String authorityCodeString;

    public String getAuthorityCodeString() {
        return authorityCodeString;
    }

    public void setAuthorityCodeString(String authorityCodeString) {
        this.authorityCodeString = authorityCodeString;
    }

    @DatabaseField(defaultValue = "DATE OF ISSUE")
    private String dateOfIssueString;

    public String getDateOfIssueString() {
        return dateOfIssueString;
    }

    public void setDateOfIssueString(String dateOfIssueString) {
        this.dateOfIssueString = dateOfIssueString;
    }

    @DatabaseField(defaultValue = "DATE OF EXPIRY")
    private String dateOfExpiryString;

    public String getDateOfExpiryString() {
        return dateOfExpiryString;
    }

    public void setDateOfExpiryString(String dateOfExpiryString) {
        this.dateOfExpiryString = dateOfExpiryString;
    }

    @DatabaseField(defaultValue = "NATIONALITY")
    private String nationalityString;

    public String getNationalityString() {
        return nationalityString;
    }

    public void setNationalityString(String dateOfExpiryString) {
        this.nationalityString = dateOfExpiryString;
    }
}