# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this frontend project.

## Project Overview

This is a temperature tracking system with PostgreSQL database backend, containerized using Docker Compose. The application manages time-series data organized into series, with user authentication and role-based access control. There are two roles: USER who can only preview the data ADMIN who can add, edit and preview data.

## Application Architecture

Frontend application is provided in Angular 20.