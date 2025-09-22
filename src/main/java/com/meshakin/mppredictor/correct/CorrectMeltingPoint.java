package com.meshakin.mppredictor.correct;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;

/// After we found id with melting point we can see value is not in one format. We also can meet trash data in value.
/// The class corrects format of melting point.
///
/// There are some main format of writing melting point:
/// 1. x Unit (277 °C)
/// 2. x-y Unit (174-179 °C)
/// 3. x to y Unit (-9.2 to -9.1 °C)
public class CorrectMeltingPoint {
    public static void main(String[] args) {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader("./cvs_files/cid_and_melting_point_from_pubchem.cvs"));
             FileWriter fileWriter = new FileWriter("./cvs_files/cid_and_corrected_melting_point.cvs")) {
            // first string = cid,mp
            fileWriter.append(bufferedReader.readLine().concat("\n"));

            String line = bufferedReader.readLine();
            String meltingPoint;
            while (line != null) {
                if (line.contains("N/A")) {
                    line = bufferedReader.readLine();
                    continue;
                }
                String[] tokens = line.split(",");
                meltingPoint = tokens[1];

                meltingPoint = meltingPoint
                        .replace("(decomposes)", "")
                        .replace("Decomposes", "")
                        .replace("Melting point equals", "")
                        .replace("Freezing point", "")
                        .replace("Â°", "")
                        .replace("−", "-")
                        .replace(">", "")
                        .replace("<", "")
                        .replaceAll("\\s+", " ")
                        .trim();

                if (meltingPoint.length() > 22) {
                    line = bufferedReader.readLine();
                    continue;
                }

                if(!meltingPoint.contains("C")&&
                   !meltingPoint.contains("K")&&
                   !meltingPoint.contains("F")) {
                    line = bufferedReader.readLine();
                    continue;
                }

                if(meltingPoint.contains("C")&&
                   meltingPoint.contains("F")) {
                    if(meltingPoint.indexOf("C") < meltingPoint.indexOf("F")) {
                        meltingPoint = meltingPoint.substring(0, meltingPoint.indexOf("C") + 1);
                    } else {
                        meltingPoint = meltingPoint.substring(meltingPoint.indexOf("F"));
                    }
                }

                String convertedTemp = convertTemperature(meltingPoint);
                if (convertedTemp == null) {
                    line = bufferedReader.readLine();
                    continue;
                }
                convertedTemp = convertedTemp.replace(',','.');


                fileWriter.write(tokens[0].concat(",").concat(convertedTemp).concat("\n"));
                line = bufferedReader.readLine();
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private static String convertTemperature(String tempStr) {
        try {
            boolean isRange = tempStr.contains("-") || tempStr.toLowerCase().contains(" to ");
            double tempValue;

            if (isRange) {
                String rangePart = tempStr.replace(" to ", "-").split("[CFK]")[0];
                String[] rangeValues = rangePart.split("-");

                if (rangeValues.length < 2) {
                    return null;
                }

                double temp1 = Double.parseDouble(rangeValues[0].replaceAll("[^0-9.-]", "").trim());
                double temp2 = Double.parseDouble(rangeValues[1].replaceAll("[^0-9.-]", "").trim());

                if (Math.abs(temp1 - temp2) > 10) {
                    return null;
                }

                tempValue = (temp1 + temp2) / 2.0;
            } else {
                String numberPart = tempStr.replaceAll("[^0-9.-]", " ").trim().split("\\s+")[0];
                tempValue = Double.parseDouble(numberPart);
            }

            if (tempStr.contains("F")) {
                tempValue = (tempValue - 32) * 5.0 / 9.0;
            } else if (tempStr.contains("K")) {
                tempValue = tempValue - 273.15;
            }


            return String.format("%.2f", tempValue);

        } catch (Exception e) {
            return null;
        }
    }
}