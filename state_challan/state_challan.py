import requests
import pandas as pd
import json

def call_challan_api(vehicle_number: str, city: str):
    base_url = "http://localhost:8080/challan"
    params = {
        "vehicleNumber": vehicle_number,
        "city": city
    }
    
    try:
        response = requests.get(base_url, params=params)
        response.raise_for_status()  
        
        print(f"--- API Call for {city} - {vehicle_number} ---")
        
        try:
            json_response = response.json()
            if isinstance(json_response, list):
                print(json.dumps(json_response, indent=4))
            else:
                print(json_response)
        except json.JSONDecodeError:
            print(response.text)
            
        print("-" * 30)

    except requests.exceptions.RequestException as e:
        print(f"Error calling API for {city} - {vehicle_number}: {e}")
        print("-" * 30)

if __name__ == "__main__":
    csv_file = "/Users/ayushroy04/Downloads/surat_challan/state_challan/surat_vehicle.csv"
    try:
        df = pd.read_csv(csv_file)
        for index, row in df.iterrows():
            call_challan_api(row['vehicleNum'], row['city'])
    except FileNotFoundError:
        print(f"Error: The file '{csv_file}' was not found.")
    except Exception as e:
        print(f"An unexpected error occurred: {e}")