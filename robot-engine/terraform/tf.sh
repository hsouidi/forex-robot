#!/usr/bin/env bash

# How to: . tf.sh CMD ENVIRONMENT_NAME

CMD=$1
STAGE=$2

if [[ ! -d "environments/${STAGE}" ]]; then
    echo "The environment '${STAGE}' doesn't exist under environments/ - please check the spelling!"
    echo "These environments are available:"
    ls environments/
    return 1
fi

if [ ${CMD} == "init" ]; then
    if [[ -f environments/${STAGE}/backend.config ]]; then
       # Configure the Backend
       echo "Running: terraform init -backend=true -backend-config=environments/${STAGE}/backend.config ."
       terraform init -backend=true -backend-config=environments/uat/backend.config
     else
       echo "The backend configuration is missing at environments/${STAGE}/backend.config!"
       return 2
    fi
fi

if [ "${CMD}" = "apply" ] || [ "${CMD}" = "destroy" ] || [ "${CMD}" = "plan" ] ; then
     if [[ -f "environments/${STAGE}/variables.tfvars" ]]; then
         terraform ${CMD} ${@:3} -var-file=environments/${STAGE}/variables.tfvars
     else
         echo "Couldn't find the variables file here: environments/${STAGE}/variables.tfvars "
         echo "Won't set up the environment ${STAGE} !"
         return 3
     fi

fi